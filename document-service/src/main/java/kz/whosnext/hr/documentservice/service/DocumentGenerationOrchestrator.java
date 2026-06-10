package kz.whosnext.hr.documentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentGenerationOrchestrator {

    private final MinioService minioService;
    private final DocumentGeneratorService docxGenerator;
    private final HtmlDocumentGenerator htmlGenerator;

    /**
     * Generates a document from a MinIO template and returns raw bytes
     * (DOCX for DOCX templates, PDF for HTML templates).
     * Use {@link #convertToPdf} when you always need a PDF.
     */
    public byte[] generateFromTemplate(String bucket, String templateKey,
                                        Map<String, String> placeholders, String contentType) {
        if (isHtml(contentType, templateKey)) {
            return generateFromHtmlTemplate(bucket, templateKey, placeholders);
        }
        return docxGenerator.generateFromTemplate(bucket, templateKey, placeholders);
    }

    /**
     * Generates a document from a MinIO template and converts it to PDF.
     * <p>
     * Previously this method returned raw DOCX bytes for DOCX templates because
     * {@code convertDocxToPdf} was never called. Fixed: DOCX branch now converts
     * the filled template to PDF before returning.
     */
    public byte[] convertToPdf(String bucket, String templateKey,
                                Map<String, String> placeholders, String contentType) {
        if (isHtml(contentType, templateKey)) {
            return generateFromHtmlTemplate(bucket, templateKey, placeholders);
        }
        // Fill DOCX placeholders first, then convert to PDF
        byte[] docxBytes = docxGenerator.generateFromTemplate(bucket, templateKey, placeholders);
        return docxGenerator.convertDocxToPdf(docxBytes);
    }

    private byte[] generateFromHtmlTemplate(String bucket, String templateKey,
                                             Map<String, String> placeholders) {
        try (InputStream templateStream = minioService.downloadFile(bucket, templateKey)) {
            String htmlContent = new String(templateStream.readAllBytes(), StandardCharsets.UTF_8);
            String processedHtml = htmlGenerator.replacePlaceholders(htmlContent, placeholders);
            return htmlGenerator.convertHtmlToPdf(processedHtml);
        } catch (Exception e) {
            log.error("Ошибка генерации из HTML-шаблона {}: {}", templateKey, e.getMessage());
            throw new RuntimeException("Ошибка генерации документа из HTML", e);
        }
    }

    public byte[] embedQrCodeInPdf(byte[] pdfBytes, byte[] qrCodeBytes) {
        return htmlGenerator.embedQrCodeInPdf(pdfBytes, qrCodeBytes);
    }

    public byte[] embedQrCodeInPdf(byte[] pdfBytes, byte[] qrCodeBytes, float x, float y, float size) {
        return htmlGenerator.embedQrCodeInPdf(pdfBytes, qrCodeBytes, x, y, size);
    }

    public byte[] convertDocxToPdf(byte[] docxBytes) {
        return docxGenerator.convertDocxToPdf(docxBytes);
    }

    public byte[] generateFromHtmlString(String html) {
        return htmlGenerator.convertHtmlToPdf(html);
    }

    private boolean isHtml(String contentType, String templateKey) {
        return (contentType != null && contentType.contains("html")) || templateKey.endsWith(".html");
    }
}
