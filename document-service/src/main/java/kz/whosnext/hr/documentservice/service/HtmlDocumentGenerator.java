package kz.whosnext.hr.documentservice.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.layout.font.FontProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class HtmlDocumentGenerator {

    @Value("${app.pdf.cyrillic-font-path:/usr/share/fonts/dejavu/DejaVuSans.ttf}")
    private String cyrillicFontPath;

    public byte[] convertHtmlToPdf(String htmlContent) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            FontProvider fontProvider = new FontProvider();
            PdfFont cyrillicFont = loadCyrillicFont();
            if (cyrillicFont != null) {
                fontProvider.addFont(cyrillicFont.getFontProgram(), PdfEncodings.IDENTITY_H);
            }
            // Add system fonts so iText can find Cyrillic glyphs from the environment
            fontProvider.addSystemFonts();

            com.itextpdf.html2pdf.ConverterProperties props = new com.itextpdf.html2pdf.ConverterProperties()
                    .setFontProvider(fontProvider)
                    .setBaseUri("http://localhost/");

            // HtmlConverter manages its own PdfDocument lifecycle — do NOT pass a separate Document
            HtmlConverter.convertToPdf(htmlContent, baos, props);

            byte[] pdfBytes = baos.toByteArray();
            log.info("HTML конвертирован в PDF, размер: {} байт", pdfBytes.length);
            return pdfBytes;
        } catch (Exception e) {
            log.error("Ошибка конвертации HTML в PDF: {}", e.getMessage());
            throw new RuntimeException("Ошибка конвертации HTML в PDF", e);
        }
    }

    public String replacePlaceholders(String html, Map<String, String> placeholders) {
        if (html == null || placeholders == null || placeholders.isEmpty()) {
            return html;
        }
        String result = html;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? escapeHtml(entry.getValue()) : "");
        }
        return result;
    }

    public byte[] embedQrCodeInPdf(byte[] pdfBytes, byte[] qrCodeBytes) {
        return embedQrCodeInPdf(pdfBytes, qrCodeBytes, 450, 50, 80);
    }

    public byte[] embedQrCodeInPdf(byte[] pdfBytes, byte[] qrCodeBytes, float x, float y, float size) {
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
            PdfWriter pdfWriter = new PdfWriter(result);
            PdfDocument pdfDoc = new PdfDocument(reader, pdfWriter);

            PdfPage lastPage = pdfDoc.getLastPage();
            PdfCanvas canvas = new PdfCanvas(lastPage);

            canvas.addImageFittedIntoRectangle(
                    ImageDataFactory.create(qrCodeBytes),
                    new Rectangle(x, y, size, size),
                    false);
            canvas.release();
            pdfDoc.close();

            return result.toByteArray();
        } catch (Exception e) {
            log.error("Ошибка вставки QR-кода в PDF: {}", e.getMessage());
            throw new RuntimeException("Ошибка вставки QR-кода", e);
        }
    }

    private PdfFont loadCyrillicFont() throws IOException {
        String[] paths = {
                cyrillicFontPath,
                // Windows
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/times.ttf",
                "C:/Windows/Fonts/calibri.ttf",
                // Linux
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/TTF/DejaVuSans.ttf",
                "/usr/share/fonts/dejavu-sans/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/freefont/FreeSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                "/usr/share/fonts/liberation-sans/LiberationSans-Regular.ttf"
        };
        for (String path : paths) {
            if (path != null && new File(path).exists()) {
                log.debug("Загружен шрифт с кириллицей: {}", path);
                return PdfFontFactory.createFont(path, PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            }
        }
        log.warn("Не найден шрифт с поддержкой кириллицы! Используем Helvetica.");
        return null;
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
