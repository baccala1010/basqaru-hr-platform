package kz.whosnext.hr.documentservice.service;

import jakarta.persistence.EntityManager;
import kz.whosnext.hr.documentservice.exception.ResourceNotFoundException;
import kz.whosnext.hr.documentservice.model.enums.DocumentStatus;
import kz.whosnext.hr.documentservice.model.enums.DocumentType;
import kz.whosnext.hr.documentservice.model.entity.Document;
import kz.whosnext.hr.documentservice.model.enums.ActivityAction;
import kz.whosnext.hr.documentservice.model.enums.ActivitySource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final TemplateService templateService;
    private final DocumentGeneratorService generatorService;
    private final QrCodeService qrCodeService;
    private final MinioService minioService;
    private final ActivityLogProducer activityLogProducer;
    private final EntityManager entityManager;

    private static final String DOCUMENTS_BUCKET  = "hr-documents";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    // Unicode-safe defaults — work regardless of properties file encoding
    @Value("${app.company.bin:240340027815}")
    private String companyBin;

    @Value("${app.company.name:ТОО «АлфаТех»}")
    private String companyName;

    @Value("${app.company.director.name:Сейткали Ермек Болатович}")
    private String directorName;

    @Value("${app.company.director.iin:850101300123}")
    private String directorIin;

    @Value("${app.company.city:г. Астана}")
    private String companyCity;

    @Transactional
    public Document generate(UUID employeeId, String certificateType,
                             String employeeFullName, String position,
                             String department, String hireDate,
                             UUID actorId, String actorEmail, String actorRole) {

        DocumentType type = DocumentType.valueOf(certificateType);

        LocalDate today = LocalDate.now();
        String[] monthsRu = {"", "января", "февраля", "марта", "апреля", "мая", "июня",
                "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        String issueDateStr = today.getDayOfMonth() + " " + monthsRu[today.getMonthValue()] + " " + today.getYear() + " г.";
        int seq = (int)(Math.abs(System.currentTimeMillis() % 99999)) + 1;
        String certNum = "СПР-" + today.getYear() + "-" + String.format("%05d", seq);

        // Persist entity first to obtain docId for the verification URL
        Document doc = Document.builder()
                .candidateId(employeeId)
                .documentType(type)
                .status(DocumentStatus.SIGNED)
                .fileName(type.name().toLowerCase() + "_" + employeeFullName + ".pdf")
                .minioObjectKey("pending")
                .contentType(PDF_CONTENT_TYPE)
                .fileSize(0L)
                .build();
        entityManager.persist(doc);
        entityManager.flush();
        UUID docId = doc.getId();

        String verifyUrl = qrCodeService.getVerifyUrl(docId);

        // Step 1: Generate filled DOCX (from MinIO template or fallback generator)
        byte[] docxBytes;
        try {
            var template = templateService.findActiveByType(type);
            String key = template.getMinioObjectKey();
            docxBytes = generatorService.generateFromTemplate(
                    "hr-templates", key, buildPlaceholders(employeeFullName, position, hireDate));
            log.info("Справка из DOCX-шаблона MinIO: type={}", type);
        } catch (ResourceNotFoundException e) {
            // For SALARY_CERTIFICATE and CONTRACT_COPY, try WORK_CERTIFICATE template as fallback
            if (type == DocumentType.SALARY_CERTIFICATE || type == DocumentType.CONTRACT_COPY) {
                try {
                    var wcTemplate = templateService.findActiveByType(DocumentType.WORK_CERTIFICATE);
                    docxBytes = generatorService.generateFromTemplate(
                            "hr-templates", wcTemplate.getMinioObjectKey(),
                            buildPlaceholders(employeeFullName, position, hireDate));
                    log.info("Используем шаблон WORK_CERTIFICATE для type={}", type);
                } catch (Exception e2) {
                    log.warn("Fallback-шаблон не найден для {}, генерируем DOCX: {}", type, e2.getMessage());
                    docxBytes = buildDocxCertificate(employeeFullName, position, department,
                            hireDate, certNum, issueDateStr, verifyUrl);
                }
            } else {
                log.info("Шаблон не найден для {}, генерируем DOCX: {}", type, e.getMessage());
                docxBytes = buildDocxCertificate(employeeFullName, position, department,
                        hireDate, certNum, issueDateStr, verifyUrl);
            }
        } catch (Exception e) {
            log.warn("Ошибка загрузки шаблона {}, генерируем DOCX: {}", type, e.getMessage());
            docxBytes = buildDocxCertificate(employeeFullName, position, department,
                    hireDate, certNum, issueDateStr, verifyUrl);
        }

        // Step 2: Generate QR code for PDF embedding
        byte[] qrCodeBytes = null;
        try {
            qrCodeBytes = qrCodeService.generateVerificationQrCode(docId, 120, 120);
        } catch (Exception e) {
            log.warn("QR-код не сгенерирован (продолжаем без него): {}", e.getMessage());
        }

        // Step 3: Convert DOCX → PDF with ЭЦП block appended
        byte[] pdfBytes = generatorService.generatePdfFromDocxWithEds(
                docxBytes, docId.toString(), companyName, companyBin, verifyUrl,
                companyCity, qrCodeBytes);

        String objectKey = "certificates/" + employeeId + "/"
                + type.name().toLowerCase() + "_" + docId + ".pdf";
        minioService.uploadBytes(DOCUMENTS_BUCKET, objectKey, pdfBytes, PDF_CONTENT_TYPE);

        doc.setMinioObjectKey(objectKey);
        doc.setFileSize((long) pdfBytes.length);
        doc.setVerificationQrUrl(verifyUrl);
        doc.setSignatureQrUrl(qrCodeService.getSignatureUrl(docId));
        doc.setQrCodeUrl(verifyUrl);

        log.info("PDF справка с ЭЦП сгенерирована: type={}, employeeId={}, id={}", type, employeeId, docId);

        activityLogProducer.log(
                ActivityAction.CERTIFICATE_GENERATED, ActivitySource.DOCUMENT_SERVICE,
                actorId, actorEmail, actorRole,
                "Certificate", docId.toString(),
                "Certificate generated: type=" + certificateType + ", employeeId=" + employeeId);

        return doc;
    }

    private byte[] buildDocxCertificate(String employeeFullName, String position, String department,
                                         String hireDate, String certNum, String issueDateStr,
                                         String verifyUrl) {
        return generatorService.generateCertificateDocx(
                companyName, companyBin, companyCity,
                directorName, certNum, issueDateStr,
                employeeFullName, position, department,
                formatHireDate(hireDate), verifyUrl);
    }

    // ── Placeholder map for work_certificate.docx ────────────────────────────────────────

    private Map<String, String> buildPlaceholders(String employeeFullName, String position, String hireDate) {
        LocalDate today = LocalDate.now();
        String[] monthsRu = {"", "января", "февраля", "марта", "апреля", "мая", "июня",
                "июля", "августа", "сентября", "октября", "ноября", "декабря"};

        String hireDateFormatted = formatHireDate(hireDate);
        String shortCompanyName = companyName
                .replaceAll("(?i)ТОО\\s*", "").replaceAll("[\"«»]", "").trim();

        Map<String, String> p = new LinkedHashMap<>();

        p.put("dd.mm.yyyy", hireDateFormatted);
        p.put("dd month yyyy", today.getDayOfMonth() + " "
                + monthsRu[today.getMonthValue()] + " " + today.getYear());

        p.put("ФИО",       employeeFullName != null ? employeeFullName : "___________");
        p.put("ДОЛЖНОСТЬ", position != null ? position : "___________");
        p.put("Должность", position != null ? position : "___________");
        p.put("ОТДЕЛ",     "___________");

        p.put("dd",    String.valueOf(today.getDayOfMonth()));
        p.put("month", monthsRu[today.getMonthValue()]);
        p.put("yyyy",  String.valueOf(today.getYear()));

        p.put("КОМПАНИЯ",      companyName);
        p.put("БИН",           companyBin);
        p.put("ДИРЕКТОР",      directorName);
        p.put("ЖСН ДИРЕКТОРА", directorIin);

        // Full company name patterns (must come before partial 'iQadam' replacements)
        p.put("TOO «iQadam Systems»",            companyName);
        p.put("ТОО «iQadam Systems»",            companyName);
        p.put("TOO \"iQadam Systems\"",          companyName);
        p.put("ТОО \"iQadam Systems\"",          companyName);
        p.put("iQadam Systems",                  shortCompanyName);
        p.put("iQadamSystems",                   shortCompanyName);
        p.put("iQadam",                          shortCompanyName);
        p.put("240940030409",                    companyBin);
        p.put("г. Астана",                       companyCity);
        p.put("Астана қ .",                      companyCity);
        p.put("Хаймульдина Гулстан Мугаловна",   directorName);
        p.put("Хаймульдина Г . М .",             directorName);
        p.put("Хаймульдина Г. М.",               directorName);
        p.put("Хаймульдина Г.М.",                directorName);
        p.put("Хаймульдина Г.М",                 directorName);

        return p;
    }

    private String formatHireDate(String hireDate) {
        if (hireDate == null || hireDate.isBlank()) return "___________";
        try {
            return LocalDate.parse(hireDate).format(DATE_FMT);
        } catch (Exception e) {
            return hireDate;
        }
    }

}
