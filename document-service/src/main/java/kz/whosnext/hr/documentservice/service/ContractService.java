package kz.whosnext.hr.documentservice.service;

import kz.whosnext.hr.documentservice.event.DocumentGeneratedEvent;
import kz.whosnext.hr.documentservice.event.DocumentSignedEvent;
import kz.whosnext.hr.documentservice.exception.AccessDeniedException;
import kz.whosnext.hr.documentservice.exception.BadRequestException;
import kz.whosnext.hr.documentservice.exception.ResourceNotFoundException;
import kz.whosnext.hr.documentservice.feign.CandidateFeignClient;
import kz.whosnext.hr.documentservice.model.dto.response.DocumentResponse;
import kz.whosnext.hr.documentservice.model.entity.Document;
import kz.whosnext.hr.documentservice.model.enums.ActivityAction;
import kz.whosnext.hr.documentservice.model.enums.ActivitySource;
import kz.whosnext.hr.documentservice.model.entity.DocumentTemplate;
import kz.whosnext.hr.documentservice.model.enums.DocumentStatus;
import kz.whosnext.hr.documentservice.model.enums.DocumentType;
import kz.whosnext.hr.documentservice.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final MinioService minioService;
    private final TemplateService templateService;
    private final DocumentGeneratorService generatorService;
    private final DocumentGenerationOrchestrator orchestrator;
    private final QrCodeService qrCodeService;
    private final NcaNodeService ncaNodeService;
    private final KafkaProducerService kafkaProducerService;
    private final CandidateFeignClient candidateFeignClient;
    private final ActivityLogProducer activityLogProducer;

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

    @Value("${app.ecp.skip-candidate-verify:true}")
    private boolean skipCandidateVerify;

    private static final String DOCUMENTS_BUCKET = "hr-documents";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Transactional
    public List<DocumentResponse> generatePackage(UUID applicationId, UUID candidateId, UUID vacancyId,
                                                   String firstName, String lastName, String iin,
                                                   String position, String contractNumber, UUID candidateUserId) {
        log.info("Генерация пакета документов: applicationId={}", applicationId);

        String num = contractNumber != null ? contractNumber : String.valueOf(System.currentTimeMillis() % 10000);

        LocalDate today = LocalDate.now();
        String[] monthsRu = {"", "января", "февраля", "марта", "апреля", "мая", "июня",
                "июля", "августа", "сентября", "октября", "ноября", "декабря"};

        // Short company name without the legal form prefix (for templates that write
        // "жауапкершілігі шектеулі серіктестігі «<name>»" in Kazakh)
        String shortCompanyName = companyName
                .replaceAll("(?i)ТОО\\s*", "").replaceAll("[\"«»]", "").trim();

        Map<String, String> placeholders = new LinkedHashMap<>();

        // ── 1. Compound date patterns FIRST (must precede standalone dd / month / yyyy) ──
        placeholders.put("dd.mm.yyyy", today.format(DATE_FMT));
        placeholders.put("dd month yyyy", formatDateKz(today));

        // ── 2. Document / contract number ────────────────────────────────────────────────
        placeholders.put("num", num);
        placeholders.put("номер", num);

        // ── 3. Candidate personal data ────────────────────────────────────────────────────
        placeholders.put("ФИО", lastName + " " + firstName);
        placeholders.put("ДОЛЖНОСТЬ", position != null ? position : "___________");
        // Kazakh employment_contract.docx uses "Должность" (mixed case)
        placeholders.put("Должность", position != null ? position : "___________");
        placeholders.put("ЖСН номер", iin != null ? iin : "____________");

        // ── 4. Individual date parts (after compound patterns to avoid partial matches) ──
        placeholders.put("dd",    String.valueOf(today.getDayOfMonth()));
        placeholders.put("month", monthsRu[today.getMonthValue()]);
        placeholders.put("yyyy",  String.valueOf(today.getYear()));

        // ── 5. Company details (generic placeholders) ────────────────────────────────────
        placeholders.put("КОМПАНИЯ",      companyName);
        placeholders.put("БИН",           companyBin);
        placeholders.put("ДИРЕКТОР",      directorName);
        placeholders.put("ЖСН ДИРЕКТОРА", directorIin);

        // ── 6. Replace hardcoded iQadam-Systems values from the original templates ────────
        // The DOCX files were created for "iQadam Systems" / "Хаймульдина Г.М.".
        // We replace them with the values from application.properties at runtime.
        placeholders.put("iQadam Systems",                shortCompanyName);
        placeholders.put("iQadamSystems",                 shortCompanyName);
        placeholders.put("iQadam",                        shortCompanyName);
        placeholders.put("240940030409",                  companyBin);
        placeholders.put("Хаймульдина Гулстан Мугаловна", directorName);
        placeholders.put("Хаймульдина Г . М .",           directorName);
        placeholders.put("Хаймульдина Г. М.",             directorName);
        placeholders.put("Хаймульдина Г.М.",              directorName);
        placeholders.put("Г. Хаймульдиной",               directorName);
        placeholders.put("Хаймульдине Г . М .",           directorName);

        List<DocumentType> typesToGenerate = List.of(
                DocumentType.EMPLOYMENT_CONTRACT,
                DocumentType.NDA,
                DocumentType.PERSONAL_DATA_CONSENT,
                DocumentType.ORDER
        );

        List<UUID> documentIds = new ArrayList<>();

        for (DocumentType type : typesToGenerate) {
            try {
                byte[] pdfBytes;
                try {
                    DocumentTemplate template = templateService.findActiveByType(type);
                    pdfBytes = orchestrator.convertToPdf(
                            "hr-templates", template.getMinioObjectKey(), placeholders,
                            template.getContentType());
                } catch (ResourceNotFoundException e) {
                    log.warn("Шаблон не найден для типа {}, используем встроенный: {}", type, e.getMessage());
                    pdfBytes = generateFallbackPdf(type, placeholders);
                } catch (Exception e) {
                    log.warn("Ошибка загрузки шаблона для типа {}, используем встроенный: {}", type, e.getMessage());
                    pdfBytes = generateFallbackPdf(type, placeholders);
                }

                String fileName = type.name().toLowerCase() + "_" + lastName + "_" + firstName + ".pdf";

                Document doc = Document.builder()
                        .applicationId(applicationId)
                        .candidateId(candidateId)
                        .vacancyId(vacancyId)
                        .uploadedBy(candidateUserId)
                        .documentType(type)
                        .status(DocumentStatus.PENDING_SIGNATURE)
                        .fileName(fileName)
                        .minioObjectKey("contracts/pending/" + UUID.randomUUID() + ".pdf")
                        .contentType("application/pdf")
                        .fileSize(0L)
                        .signerIin(iin)
                        .build();

                doc = documentRepository.save(doc);
                UUID docId = doc.getId();

                byte[] verificationQrCode = qrCodeService.generateVerificationQrCode(docId, 200, 200);
                byte[] signatureQrCode = qrCodeService.generateSignatureQrCode(docId, 200, 200);
                pdfBytes = orchestrator.embedQrCodeInPdf(pdfBytes, verificationQrCode, 450, 50, 80);
                pdfBytes = orchestrator.embedQrCodeInPdf(pdfBytes, signatureQrCode, 360, 50, 80);

                String objectKey = "contracts/" + applicationId + "/" + type.name().toLowerCase() + "_" + docId + ".pdf";
                minioService.uploadBytes(DOCUMENTS_BUCKET, objectKey, pdfBytes, "application/pdf");

                doc.setMinioObjectKey(objectKey);
                doc.setFileSize((long) pdfBytes.length);
                doc.setVerificationQrUrl(qrCodeService.getVerifyUrl(docId));
                doc.setSignatureQrUrl(qrCodeService.getSignatureUrl(docId));
                doc.setQrCodeUrl(qrCodeService.getVerifyUrl(docId));
                documentRepository.save(doc);

                documentIds.add(docId);
                log.info("Документ сгенерирован: type={}, id={}", type, docId);
            } catch (Exception e) {
                log.error("Критическая ошибка генерации документа типа {}: {}", type, e.getMessage(), e);
            }
        }

        kafkaProducerService.sendDocumentGenerated(new DocumentGeneratedEvent(applicationId, documentIds));
        log.info("Пакет документов сгенерирован: applicationId={}, count={}", applicationId, documentIds.size());

        activityLogProducer.log(
                ActivityAction.DOCUMENT_GENERATED, ActivitySource.DOCUMENT_SERVICE,
                null, null, "SYSTEM",
                "ContractPackage", applicationId.toString(),
                "Contract package generated: applicationId=" + applicationId + ", count=" + documentIds.size());

        return documentRepository.findByApplicationId(applicationId).stream()
                .map(documentService::toResponseWithUrl)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getContractsByApplication(UUID applicationId, UUID userId, String role) {
        if ("CANDIDATE".equals(role)) {
            List<Document> docs = documentRepository.findByApplicationId(applicationId);
            // Original check (candidateId = candidate profile UUID, not auth user UUID — may mismatch):
            // if (docs.stream().noneMatch(d -> d.getCandidateId() != null && d.getCandidateId().equals(userId))) {
            //     throw new AccessDeniedException("Нет доступа к документам этой заявки");
            // }
            // Extended check: also accept uploadedBy (auth user UUID stored on document creation)
            boolean hasAccess = docs.isEmpty() || docs.stream().anyMatch(d ->
                (d.getCandidateId() != null && d.getCandidateId().equals(userId)) ||
                (d.getUploadedBy() != null && d.getUploadedBy().equals(userId))
            );
            if (!hasAccess) {
                throw new AccessDeniedException("Нет доступа к документам этой заявки");
            }
        }
        return documentRepository.findByApplicationId(applicationId).stream()
                .map(documentService::toResponseWithUrl)
                .toList();
    }

    @Transactional
    public DocumentResponse signDocument(UUID documentId, UUID userId, String ipAddress) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Документ не найден: " + documentId));

        if (doc.getStatus() != DocumentStatus.PENDING_SIGNATURE)
            throw new BadRequestException("Документ не ожидает подписания, текущий статус: " + doc.getStatus());

        doc.setStatus(DocumentStatus.SIGNED);
        doc.setSignedAt(LocalDateTime.now());
        doc.setSignedBy(userId);
        doc.setSignatureData("simple-sign|userId=" + userId + "|ip=" + ipAddress + "|at=" + LocalDateTime.now());
        doc.setSignatureHash(calculateSha256(doc.getSignatureData()));

        updateQrCodesAfterSigning(doc);

        documentRepository.save(doc);
        log.info("Документ подписан (простая подпись): id={}, userId={}", documentId, userId);

        activityLogProducer.log(
                ActivityAction.DOCUMENT_SIGNED, ActivitySource.DOCUMENT_SERVICE,
                userId, null, null,
                "Document", documentId.toString(),
                "Document signed (simple): " + doc.getDocumentType());

        checkAllSigned(doc.getApplicationId(), doc.getCandidateId());
        return documentService.toResponseWithUrl(doc);
    }

    @Transactional
    public DocumentResponse signWithEcp(UUID documentId, String candidateSignedXml, UUID userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Документ не найден: " + documentId));

        if (doc.getStatus() != DocumentStatus.PENDING_SIGNATURE)
            throw new BadRequestException("Документ не ожидает подписания");

        String dualSignedXml;
        try {
            dualSignedXml = ncaNodeService.counterSignWithCompanyKey(candidateSignedXml, skipCandidateVerify);
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            log.error("Ошибка обработки ЭЦП для документа {}: {}", documentId, msg);

            if (!skipCandidateVerify && (msg.contains("невалидна") || msg.contains("invalid"))) {
                throw new BadRequestException(
                        "Подпись ЭЦП не прошла верификацию. " +
                        "Убедитесь, что используете действительный сертификат НУЦ РК.");
            }
            if (skipCandidateVerify) {
                // Dev/demo mode: NCANode or company key unavailable — accept candidate's signed XML as-is
                log.warn("Dev mode: контрподпись компании недоступна, сохраняем подпись кандидата без контрподписи: {}", msg);
                dualSignedXml = candidateSignedXml;
            } else {
                throw new BadRequestException(
                        "Сервис проверки ЭЦП временно недоступен. " +
                        "Проверьте, что NCANode запущен, и попробуйте снова.");
            }
        }

        String signedKey = doc.getMinioObjectKey().replace(".pdf", "_signed.xml");
        minioService.uploadBytes(DOCUMENTS_BUCKET, signedKey, dualSignedXml.getBytes(StandardCharsets.UTF_8), "application/xml; charset=UTF-8");

        doc.setStatus(DocumentStatus.SIGNED);
        doc.setSignedAt(LocalDateTime.now());
        doc.setSignedBy(userId);
        doc.setSignatureData(dualSignedXml);
        doc.setSignatureHash(calculateSha256(dualSignedXml));

        updateQrCodesAfterSigning(doc);

        documentRepository.save(doc);
        log.info("Документ подписан (ЭЦП): id={}, userId={}", documentId, userId);

        activityLogProducer.log(
                ActivityAction.DOCUMENT_SIGNED, ActivitySource.DOCUMENT_SERVICE,
                userId, null, null,
                "Document", documentId.toString(),
                "Document signed (ECP): " + doc.getDocumentType());

        checkAllSigned(doc.getApplicationId(), doc.getCandidateId());
        return documentService.toResponseWithUrl(doc);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> verifyDocument(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Документ не найден: " + documentId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentId", doc.getId());
        result.put("documentType", doc.getDocumentType());
        result.put("status", doc.getStatus());
        result.put("fileName", doc.getFileName());
        result.put("createdAt", doc.getCreatedAt());
        result.put("signedAt", doc.getSignedAt());
        result.put("signedBy", doc.getSignedBy());
        result.put("signerIin", doc.getSignerIin());
        result.put("companyBin", companyBin);
        result.put("verificationQrUrl", doc.getVerificationQrUrl() != null ? doc.getVerificationQrUrl() : doc.getQrCodeUrl());
        result.put("signatureQrUrl", doc.getSignatureQrUrl());
        result.put("signatureHash", doc.getSignatureHash());
        result.put("valid", doc.getStatus() == DocumentStatus.SIGNED);

        if (doc.getSignatureData() != null && doc.getSignatureData().startsWith("<?xml")) {
            try {
                Map<String, Object> ncaVerify = ncaNodeService.xmlVerify(doc.getSignatureData());
                result.put("ecpVerification", ncaVerify);
            } catch (Exception e) {
                result.put("ecpVerification", Map.of("error", e.getMessage()));
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSignatureInfo(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Документ не найден: " + documentId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentId", doc.getId());
        result.put("status", doc.getStatus());
        result.put("signedAt", doc.getSignedAt());
        result.put("signedBy", doc.getSignedBy());
        result.put("signatureHash", doc.getSignatureHash());
        result.put("signatureQrUrl", doc.getSignatureQrUrl());
        result.put("companyBin", companyBin);
        result.put("signerIin", doc.getSignerIin());
        result.put("hasSignaturePayload", doc.getSignatureData() != null && !doc.getSignatureData().isBlank());
        return result;
    }

    @Transactional
    public List<DocumentResponse> regenerate(UUID applicationId, String role, UUID actorId, String actorEmail) {
        if (!"HR".equals(role) && !"ADMIN".equals(role))
            throw new AccessDeniedException("Повторная генерация доступна только HR/ADMIN");

        List<Document> existing = documentRepository.findByApplicationId(applicationId);

        // Fetch fresh application data from candidate-service
        Map<String, Object> appDetails;
        try {
            appDetails = candidateFeignClient.getApplicationDetails(applicationId);
        } catch (Exception e) {
            log.warn("Не удалось получить данные заявки из candidate-service: {}", e.getMessage());
            appDetails = null;
        }

        String firstName = appDetails != null ? (String) appDetails.get("firstName") : null;
        String lastName = appDetails != null ? (String) appDetails.get("lastName") : null;
        String iin = appDetails != null ? (String) appDetails.get("iin") : null;
        String position = appDetails != null ? (String) appDetails.get("positionName") : null;

        // Resolve candidateId, vacancyId and auth userId from appDetails (preferred) or existing docs (fallback)
        UUID candidateId = null;
        UUID vacancyId = null;
        UUID candidateUserId = null;
        if (appDetails != null) {
            Object cid = appDetails.get("candidateId");
            Object vid = appDetails.get("vacancyId");
            Object uid = appDetails.get("userId");
            if (cid != null) candidateId = UUID.fromString(cid.toString());
            if (vid != null) vacancyId = UUID.fromString(vid.toString());
            if (uid != null) candidateUserId = UUID.fromString(uid.toString());
        }
        if (candidateId == null && !existing.isEmpty()) {
            candidateId = existing.get(0).getCandidateId();
            vacancyId = existing.get(0).getVacancyId();
            candidateUserId = existing.get(0).getUploadedBy();
        }
        if (candidateId == null) {
            throw new ResourceNotFoundException(
                    "Не удалось определить кандидата для заявки " + applicationId +
                    ". Убедитесь, что candidate-service доступен.");
        }

        // Delete existing documents from MinIO and DB
        for (Document doc : existing) {
            try {
                minioService.deleteFile(DOCUMENTS_BUCKET, doc.getMinioObjectKey());
            } catch (Exception e) {
                log.warn("Не удалось удалить файл из MinIO: {}", doc.getMinioObjectKey());
            }
        }
        if (!existing.isEmpty()) {
            documentRepository.deleteAll(existing);
            log.info("Удалено {} старых документов для applicationId={}", existing.size(), applicationId);
        }

        activityLogProducer.log(
                ActivityAction.DOCUMENT_GENERATED, ActivitySource.DOCUMENT_SERVICE,
                actorId, actorEmail, role,
                "ContractPackage", applicationId.toString(),
                "Contract package regenerated: applicationId=" + applicationId);

        return generatePackage(applicationId, candidateId, vacancyId, firstName, lastName, iin, position, null, candidateUserId);
    }

    private void updateQrCodesAfterSigning(Document doc) {
        try (var is = minioService.downloadFile(DOCUMENTS_BUCKET, doc.getMinioObjectKey())) {
            byte[] pdfBytes = is.readAllBytes();

            String qrPayload = qrCodeService.getVerifyUrl(doc.getId())
                    + "|hash=" + (doc.getSignatureHash() != null ? doc.getSignatureHash() : "");
            byte[] signedQrCode = qrCodeService.generateQrCodeFromText(qrPayload, 200, 200);

            pdfBytes = orchestrator.embedQrCodeInPdf(pdfBytes, signedQrCode, 360, 50, 80);

            minioService.uploadBytes(DOCUMENTS_BUCKET, doc.getMinioObjectKey(), pdfBytes, "application/pdf");
            log.info("QR-код обновлён после подписания: docId={}", doc.getId());
        } catch (Exception e) {
            log.warn("Не удалось обновить QR-коды после подписания: {}", e.getMessage());
        }
    }

    private void checkAllSigned(UUID applicationId, UUID candidateId) {
        long total = documentRepository.findByApplicationId(applicationId).size();
        long signed = documentRepository.countByApplicationIdAndStatus(applicationId, DocumentStatus.SIGNED);

        if (total > 0 && total == signed) {
            log.info("Все документы подписаны: applicationId={}", applicationId);
            kafkaProducerService.sendDocumentSigned(new DocumentSignedEvent(applicationId, candidateId));
        }
    }

    private String formatDateKz(LocalDate date) {
        String[] months = {"", "января", "февраля", "марта", "апреля", "мая", "июня",
                "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        return date.getDayOfMonth() + " " + months[date.getMonthValue()] + " " + date.getYear();
    }

    private String calculateSha256(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка вычисления hash подписи", e);
        }
    }

    // Fallback PDF used when no DOCX template is found in MinIO.
    // Generates legally structured documents per RK Law No. 370-II (Electronic Document and EDS).
    private byte[] generateFallbackPdf(DocumentType type, Map<String, String> placeholders) {
        String fio      = placeholders.getOrDefault("ФИО", "___________");
        String jsn      = placeholders.getOrDefault("ЖСН номер", "____________");
        String date     = placeholders.getOrDefault("dd.mm.yyyy", LocalDate.now().format(DATE_FMT));
        String dateKz   = placeholders.getOrDefault("dd month yyyy", formatDateKz(LocalDate.now()));
        String num      = placeholders.getOrDefault("num", "—");
        String position = placeholders.getOrDefault("ДОЛЖНОСТЬ", "___________");
        String company  = placeholders.getOrDefault("КОМПАНИЯ", companyName);
        String bin      = placeholders.getOrDefault("БИН", companyBin);
        String director = placeholders.getOrDefault("ДИРЕКТОР", directorName);
        String dirJsn   = placeholders.getOrDefault("ЖСН ДИРЕКТОРА", directorIin);

        String legalNote = "Настоящий электронный документ равнозначен документу на бумажном носителе " +
                "в соответствии с Законом Республики Казахстан «Об электронном документе и " +
                "электронной цифровой подписи» от 7 января 2003 года № 370-II. " +
                "Подлинность документа подтверждается электронной цифровой подписью (ЭЦП), " +
                "выданной Национальным удостоверяющим центром Республики Казахстан (НУЦ РК).";

        String baseStyle = """
            <!DOCTYPE html><html><head><meta charset="UTF-8"/>
            <style>
              body{font-family:Arial,sans-serif;font-size:11pt;margin:50px 60px;color:#1a1a1a;}
              h2{text-align:center;font-size:13pt;text-transform:uppercase;margin-bottom:4px;}
              h3{text-align:center;font-size:11pt;font-weight:normal;margin-top:0;}
              .meta{text-align:right;font-size:10pt;color:#444;margin-bottom:20px;}
              table{width:100%;border-collapse:collapse;margin:12px 0;}
              td{padding:5px 8px;vertical-align:top;}
              td:first-child{width:45%;font-weight:bold;}
              .section{margin:16px 0 6px;font-weight:bold;font-size:11pt;border-bottom:1px solid #ccc;padding-bottom:2px;}
              .legal{font-size:8.5pt;color:#444;border:1px solid #bbb;padding:8px 12px;margin-top:30px;line-height:1.5;}
              .sign-block{margin-top:40px;}
              .sign-row{display:flex;justify-content:space-between;margin-top:20px;}
              .sign-col{width:45%;}
              .sign-line{border-bottom:1px solid #333;height:24px;margin:4px 0;}
              .sign-label{font-size:9pt;color:#555;}
              .center{text-align:center;}
              p{margin:6px 0;line-height:1.5;}
              li{margin:4px 0;line-height:1.5;}
            </style></head><body>
            """;

        String html = switch (type) {
            case EMPLOYMENT_CONTRACT -> baseStyle + """
                <h2>Трудовой договор № %s</h2>
                <h3>г. Астана, Республика Казахстан</h3>
                <div class="meta">Дата: %s</div>

                <p><b>%s</b> (далее — «Работодатель»), в лице директора <b>%s</b>, ЖСН <b>%s</b>,
                действующего на основании Устава, с одной стороны, и</p>
                <p><b>%s</b>, ЖСН <b>%s</b> (далее — «Работник»), с другой стороны,
                заключили настоящий Трудовой договор о нижеследующем:</p>

                <div class="section">1. Предмет договора</div>
                <table>
                  <tr><td>Должность:</td><td>%s</td></tr>
                  <tr><td>Место работы:</td><td>%s, г. Астана</td></tr>
                  <tr><td>Вид договора:</td><td>Бессрочный трудовой договор</td></tr>
                  <tr><td>Дата начала работы:</td><td>%s</td></tr>
                </table>

                <div class="section">2. Права и обязанности сторон</div>
                <p>2.1. Работодатель обязуется: обеспечить Работника работой в соответствии с трудовым
                договором; своевременно выплачивать заработную плату; обеспечить надлежащие условия труда.</p>
                <p>2.2. Работник обязуется: добросовестно исполнять трудовые обязанности; соблюдать
                трудовую дисциплину и внутренний трудовой распорядок; бережно относиться к имуществу
                Работодателя.</p>

                <div class="section">3. Режим труда и отдыха</div>
                <p>Работнику устанавливается пятидневная рабочая неделя с двумя выходными днями
                (суббота, воскресенье). Продолжительность рабочего дня — 8 часов.</p>

                <div class="section">4. Ответственность сторон</div>
                <p>Стороны несут ответственность за ненадлежащее исполнение обязательств по настоящему
                договору в соответствии с законодательством Республики Казахстан.</p>

                <div class="section">5. Реквизиты сторон</div>
                <table>
                  <tr><td><b>Работодатель:</b></td><td><b>Работник:</b></td></tr>
                  <tr><td>%s</td><td>%s</td></tr>
                  <tr><td>БИН: %s</td><td>ЖСН: %s</td></tr>
                  <tr><td>Директор: %s</td><td>Должность: %s</td></tr>
                </table>

                <div class="sign-block">
                  <div class="sign-row">
                    <div class="sign-col">
                      <p><b>Работодатель:</b></p>
                      <div class="sign-line"></div>
                      <div class="sign-label">%s / %s</div>
                      <div class="sign-label">ЖСН: %s</div>
                    </div>
                    <div class="sign-col">
                      <p><b>Работник:</b></p>
                      <div class="sign-line"></div>
                      <div class="sign-label">%s</div>
                      <div class="sign-label">ЖСН: %s</div>
                    </div>
                  </div>
                </div>

                <div class="legal">%s</div>
                </body></html>
                """.formatted(
                    num, dateKz,
                    company, director, dirJsn,
                    fio, jsn,
                    position, company, date,
                    company, fio,
                    bin, jsn,
                    director, position,
                    director, company, dirJsn,
                    fio, jsn,
                    legalNote);

            case NDA -> baseStyle + """
                <h2>Соглашение о неразглашении конфиденциальной информации № %s</h2>
                <h3>(NDA — Non-Disclosure Agreement)</h3>
                <h3>г. Астана, Республика Казахстан</h3>
                <div class="meta">Дата: %s</div>

                <p><b>%s</b>, БИН <b>%s</b> (далее — «Работодатель»), в лице директора <b>%s</b>, и</p>
                <p><b>%s</b>, ЖСН <b>%s</b>, принятый(ая) на должность <b>%s</b> (далее — «Работник»),
                заключили настоящее Соглашение о нижеследующем:</p>

                <div class="section">1. Предмет соглашения</div>
                <p>Работник обязуется не разглашать третьим лицам конфиденциальную информацию Работодателя,
                ставшую ему известной в связи с исполнением трудовых обязанностей.</p>

                <div class="section">2. Конфиденциальная информация</div>
                <p>К конфиденциальной информации относятся: коммерческая тайна; персональные данные
                клиентов и сотрудников; технологии, методики и бизнес-процессы; финансовые показатели;
                иная информация, обозначенная Работодателем как конфиденциальная.</p>

                <div class="section">3. Обязательства Работника</div>
                <p>3.1. Не разглашать конфиденциальную информацию в течение срока действия трудового
                договора и в течение 3 (трёх) лет после его прекращения.</p>
                <p>3.2. Использовать конфиденциальную информацию исключительно в служебных целях.</p>
                <p>3.3. Немедленно сообщать Работодателю о ставших известными фактах утечки информации.</p>

                <div class="section">4. Ответственность</div>
                <p>За нарушение настоящего Соглашения Работник несёт ответственность в соответствии
                с законодательством Республики Казахстан, включая возмещение причинённых убытков.</p>

                <div class="section">5. Срок действия</div>
                <p>Соглашение вступает в силу с момента подписания и действует в течение срока трудовых
                отношений и 3 лет после их прекращения.</p>

                <div class="sign-block">
                  <div class="sign-row">
                    <div class="sign-col">
                      <p><b>Работодатель:</b></p>
                      <div class="sign-line"></div>
                      <div class="sign-label">%s, директор</div>
                      <div class="sign-label">%s, БИН: %s</div>
                    </div>
                    <div class="sign-col">
                      <p><b>Работник:</b></p>
                      <div class="sign-line"></div>
                      <div class="sign-label">%s</div>
                      <div class="sign-label">ЖСН: %s</div>
                    </div>
                  </div>
                </div>

                <div class="legal">%s</div>
                </body></html>
                """.formatted(
                    num, dateKz,
                    company, bin, director,
                    fio, jsn, position,
                    director, company, bin,
                    fio, jsn,
                    legalNote);

            case PERSONAL_DATA_CONSENT -> baseStyle + """
                <h2>Согласие на обработку персональных данных</h2>
                <h3>г. Астана, Республика Казахстан</h3>
                <div class="meta">Дата: %s</div>

                <p>Я, <b>%s</b>, ЖСН <b>%s</b>, в соответствии с Законом Республики Казахстан
                «О персональных данных и их защите» от 21 мая 2013 года № 94-V, настоящим даю
                согласие <b>%s</b> (БИН <b>%s</b>) на обработку моих персональных данных.</p>

                <div class="section">1. Перечень персональных данных</div>
                <ul>
                  <li>Фамилия, имя, отчество</li>
                  <li>Индивидуальный идентификационный номер (ЖСН/ИИН)</li>
                  <li>Дата и место рождения</li>
                  <li>Гражданство и место регистрации</li>
                  <li>Контактная информация (телефон, адрес электронной почты)</li>
                  <li>Сведения об образовании, квалификации и опыте работы</li>
                  <li>Сведения о трудовой деятельности</li>
                  <li>Биометрические данные (фотография)</li>
                </ul>

                <div class="section">2. Цели обработки</div>
                <ul>
                  <li>Ведение кадрового учёта и заключение трудового договора</li>
                  <li>Расчёт и выплата заработной платы</li>
                  <li>Исполнение требований налогового и трудового законодательства РК</li>
                  <li>Обеспечение безопасности на рабочем месте</li>
                </ul>

                <div class="section">3. Условия обработки</div>
                <p>Обработка персональных данных осуществляется с соблюдением конфиденциальности
                и в соответствии с законодательством Республики Казахстан. Данные не передаются
                третьим лицам без отдельного согласия, за исключением случаев, предусмотренных
                законодательством.</p>

                <div class="section">4. Права субъекта персональных данных</div>
                <p>Я вправе отозвать настоящее согласие, направив письменное заявление Работодателю.
                Отзыв согласия не влечёт нарушения законных прав Работодателя по хранению данных,
                необходимых для исполнения законодательных требований.</p>

                <div class="sign-block center">
                  <p>Согласие даю добровольно, подтверждаю ознакомление с правами субъекта
                  персональных данных.</p>
                  <br/>
                  <div class="sign-line" style="max-width:300px;margin:8px auto;"></div>
                  <div class="sign-label">%s, ЖСН: %s</div>
                  <div class="sign-label">Дата: %s</div>
                </div>

                <div class="legal">%s</div>
                </body></html>
                """.formatted(
                    dateKz,
                    fio, jsn,
                    company, bin,
                    fio, jsn, date,
                    legalNote);

            case ORDER -> baseStyle + """
                <h2>Приказ № %s</h2>
                <h2>О приёме на работу</h2>
                <h3>г. Астана, Республика Казахстан</h3>
                <div class="meta">Дата: %s</div>

                <p><b>%s</b></p>
                <p>БИН: <b>%s</b></p>

                <div class="section">ПРИКАЗЫВАЮ:</div>

                <table>
                  <tr><td>Принять на работу:</td><td><b>%s</b></td></tr>
                  <tr><td>ЖСН/ИИН работника:</td><td><b>%s</b></td></tr>
                  <tr><td>Должность:</td><td><b>%s</b></td></tr>
                  <tr><td>Подразделение:</td><td>%s</td></tr>
                  <tr><td>Дата начала работы:</td><td>%s</td></tr>
                  <tr><td>Вид трудового договора:</td><td>Бессрочный</td></tr>
                  <tr><td>Основание:</td><td>Трудовой договор № %s от %s</td></tr>
                </table>

                <p>Ознакомить работника с настоящим приказом, правилами внутреннего трудового
                распорядка, должностной инструкцией и иными локальными нормативными актами.</p>

                <div class="sign-block">
                  <p><b>Директор:</b></p>
                  <div class="sign-line"></div>
                  <div class="sign-label">%s</div>
                  <div class="sign-label">ЖСН: %s</div>
                  <br/>
                  <p><b>С приказом ознакомлен(а):</b></p>
                  <div class="sign-line"></div>
                  <div class="sign-label">%s, ЖСН: %s</div>
                  <div class="sign-label">Дата: %s</div>
                </div>

                <div class="legal">%s</div>
                </body></html>
                """.formatted(
                    num, dateKz,
                    company, bin,
                    fio, jsn, position, company, date, num, date,
                    director, dirJsn,
                    fio, jsn, date,
                    legalNote);

            default -> baseStyle + """
                <h2>%s</h2>
                <div class="meta">%s</div>
                <table>
                  <tr><td>Работник:</td><td>%s</td></tr>
                  <tr><td>ЖСН:</td><td>%s</td></tr>
                  <tr><td>Работодатель:</td><td>%s</td></tr>
                </table>
                <div class="legal">%s</div>
                </body></html>
                """.formatted(type.name(), dateKz, fio, jsn, company, legalNote);
        };

        return orchestrator.generateFromHtmlString(html);
    }

    // Download a contract file — streams bytes through the API gateway so the mobile
    // app doesn't need direct MinIO access (minio:9000 is Docker-internal only).
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadFile(UUID documentId, UUID userId, String role) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Документ не найден: " + documentId));

        if ("CANDIDATE".equals(role)) {
            boolean hasAccess =
                    (doc.getCandidateId() != null && doc.getCandidateId().equals(userId)) ||
                    (doc.getUploadedBy() != null && doc.getUploadedBy().equals(userId));
            if (!hasAccess)
                throw new AccessDeniedException("Нет доступа к документу");
        }

        try (InputStream is = minioService.downloadFile(DOCUMENTS_BUCKET, doc.getMinioObjectKey())) {
            byte[] bytes = is.readAllBytes();
            String ct = doc.getContentType() != null ? doc.getContentType() : "application/pdf";
            String name = doc.getFileName() != null ? doc.getFileName() : documentId + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, ct)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name + "\"")
                    .body(bytes);
        } catch (Exception e) {
            log.error("Ошибка загрузки файла {}: {}", documentId, e.getMessage());
            throw new RuntimeException("Не удалось загрузить файл: " + e.getMessage());
        }
    }
}

