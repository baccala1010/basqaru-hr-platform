package kz.whosnext.hr.documentservice.controller;

import kz.whosnext.hr.documentservice.model.dto.request.AttendanceReportRequest;
import kz.whosnext.hr.documentservice.service.MinioService;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class AttendanceReportController {

    private final MinioService minioService;

    @Value("${app.pdf.cyrillic-font-path:/usr/share/fonts/dejavu/DejaVuSans.ttf}")
    private String cyrillicFontPath;

    private PdfFont cyrillicFont;

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            cyrillicFont = loadCyrillicFont();
        } catch (Exception e) {
            log.warn("Не удалось загрузить кириллический шрифт: {}", e.getMessage());
        }
    }

    // Цвета
    private static final DeviceRgb HEADER_BG = new DeviceRgb(37, 99, 235);   // синий
    private static final DeviceRgb HEADER_FG = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb ROW_EVEN  = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb ROW_ODD   = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb PRESENT_BG = new DeviceRgb(220, 252, 231); // зелёный
    private static final DeviceRgb ABSENT_BG  = new DeviceRgb(254, 226, 226); // красный
    private static final DeviceRgb PRESENT_FG = new DeviceRgb(22, 163, 74);
    private static final DeviceRgb ABSENT_FG  = new DeviceRgb(220, 38, 38);
    private static final DeviceRgb BORDER_CLR = new DeviceRgb(226, 232, 240);
    private static final DeviceRgb SUMMARY_BG = new DeviceRgb(239, 246, 255);

    @PostMapping("/attendance-report")
    public ResponseEntity<byte[]> generateAttendanceReport(@RequestBody AttendanceReportRequest req) {
        byte[] pdfBytes = buildAttendancePdf(req);

        String objectKey = "reports/attendance_" + req.year() + "_" + req.month() + "_" + UUID.randomUUID() + ".pdf";
        minioService.uploadBytes("hr-documents", objectKey, pdfBytes, "application/pdf");
        log.info("Отчёт посещаемости сгенерирован: {}", objectKey);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"attendance_" + req.year() + "_" + req.month() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private byte[] buildAttendancePdf(AttendanceReportRequest req) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        // Альбомная ориентация для размещения всех дней месяца
        pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
        Document document = new Document(pdfDoc);
        document.setMargins(20, 20, 20, 20);

        // ── Заголовок ─────────────────────────────────────────────────────────
        document.add(styledParagraph("ТАБЕЛЬ УЧЁТА РАБОЧЕГО ВРЕМЕНИ", 16, true, new DeviceRgb(15, 23, 42))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));

        document.add(styledParagraph(req.month() + " " + req.year() + " г.", 12, false, new DeviceRgb(100, 116, 139))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2));

        if (req.departmentName() != null && !req.departmentName().isEmpty()) {
            document.add(styledParagraph("Подразделение: " + req.departmentName(), 10, false, new DeviceRgb(71, 85, 105))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(12));
        } else {
            document.add(styledParagraph(" ", 8, false, new DeviceRgb(71, 85, 105)).setMarginBottom(8));
        }

        // ── Определяем количество дней ────────────────────────────────────────
        int dayCount = 0;
        if (req.rows() != null && !req.rows().isEmpty()) {
            dayCount = req.rows().get(0).dailyPresence().size();
        }

        // Колонки: №, ФИО, Отдел, [дни...], Итого, %
        int totalCols = 3 + dayCount + 2; // №, ФИО, Отдел + дни + Итого + %

        // Ширины колонок: фиксированные для №, ФИО, Отдел, Итого, %
        float[] colWidths = new float[totalCols];
        colWidths[0] = 20f;   // №
        colWidths[1] = 90f;   // ФИО
        colWidths[2] = 70f;   // Отдел
        float dayColWidth = Math.max(16f, (PageSize.A4.rotate().getWidth() - 200f - 40f) / Math.max(dayCount, 1));
        for (int i = 0; i < dayCount; i++) {
            colWidths[3 + i] = dayColWidth;
        }
        colWidths[totalCols - 2] = 32f; // Итого
        colWidths[totalCols - 1] = 32f; // %

        Table table = new Table(UnitValue.createPointArray(colWidths));
        table.useAllAvailableWidth();

        SolidBorder cellBorder = new SolidBorder(BORDER_CLR, 0.5f);

        // ── Заголовок таблицы ─────────────────────────────────────────────────
        table.addHeaderCell(headerCell("№", cellBorder));
        table.addHeaderCell(headerCell("ФИО", cellBorder));
        table.addHeaderCell(headerCell("Отдел", cellBorder));
        for (int i = 1; i <= dayCount; i++) {
            table.addHeaderCell(headerCell(String.valueOf(i), cellBorder));
        }
        table.addHeaderCell(headerCell("Итого", cellBorder));
        table.addHeaderCell(headerCell("%", cellBorder));

        // ── Строки данных ─────────────────────────────────────────────────────
        if (req.rows() != null) {
            int rowIdx = 0;
            for (AttendanceReportRequest.AttendanceRow row : req.rows()) {
                DeviceRgb rowBg = (rowIdx % 2 == 0) ? ROW_EVEN : ROW_ODD;

                // №
                table.addCell(dataCell(String.valueOf(rowIdx + 1), cellBorder, rowBg)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(7));

                // ФИО
                table.addCell(dataCell(
                        row.employeeName() != null ? row.employeeName() : "—",
                        cellBorder, rowBg)
                        .setFontSize(7).setBold());

                // Отдел
                table.addCell(dataCell(
                        row.department() != null ? row.department() : "—",
                        cellBorder, rowBg)
                        .setFontSize(7));

                // Дни
                for (Boolean present : row.dailyPresence()) {
                    boolean isPresent = Boolean.TRUE.equals(present);
                    Paragraph dayPara = new Paragraph(isPresent ? "✓" : "✗")
                            .setFontSize(8)
                            .setFontColor(isPresent ? PRESENT_FG : ABSENT_FG);
                    if (cyrillicFont != null) dayPara.setFont(cyrillicFont);
                    Cell dayCell = new Cell()
                            .add(dayPara)
                            .setBackgroundColor(isPresent ? PRESENT_BG : ABSENT_BG)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setVerticalAlignment(VerticalAlignment.MIDDLE)
                            .setBorder(cellBorder)
                            .setPadding(1);
                    table.addCell(dayCell);
                }

                // Итого
                table.addCell(dataCell(String.valueOf(row.totalDays()), cellBorder, SUMMARY_BG)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8).setBold());

                // %
                String pct = String.format("%.1f%%", row.attendancePercent());
                DeviceRgb pctColor = row.attendancePercent() >= 90 ? PRESENT_FG
                        : row.attendancePercent() >= 70 ? new DeviceRgb(217, 119, 6)
                        : ABSENT_FG;
                Paragraph pctPara = new Paragraph(pct).setFontSize(7).setBold().setFontColor(pctColor);
                if (cyrillicFont != null) pctPara.setFont(cyrillicFont);
                Cell pctCell = new Cell()
                        .add(pctPara)
                        .setBackgroundColor(SUMMARY_BG)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .setBorder(cellBorder)
                        .setPadding(1);
                table.addCell(pctCell);

                rowIdx++;
            }
        }

        document.add(table);

        // ── Сводка внизу ─────────────────────────────────────────────────────
        if (req.rows() != null && !req.rows().isEmpty()) {
            List<AttendanceReportRequest.AttendanceRow> rows = req.rows();
            int totalEmployees = rows.size();
            double avgPercent = rows.stream()
                    .mapToDouble(AttendanceReportRequest.AttendanceRow::attendancePercent)
                    .average().orElse(0);
            int totalPresent = rows.stream()
                    .mapToInt(AttendanceReportRequest.AttendanceRow::totalDays)
                    .sum();

            document.add(new Paragraph(" ").setFontSize(4));

            Table summary = new Table(UnitValue.createPercentArray(3)).useAllAvailableWidth();
            summary.addCell(summaryCell("Сотрудников: " + totalEmployees));
            summary.addCell(summaryCell("Всего явок: " + totalPresent));
            summary.addCell(summaryCell("Средняя посещаемость: " + String.format("%.1f%%", avgPercent)));
            document.add(summary);
        }

        // ── Подпись ───────────────────────────────────────────────────────────
        document.add(new Paragraph(" ").setFontSize(8));
        document.add(styledParagraph("Дата формирования: " + java.time.LocalDate.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                8, false, new DeviceRgb(148, 163, 184))
                .setTextAlignment(TextAlignment.RIGHT));

        document.close();
        return baos.toByteArray();
    }

    private Cell headerCell(String text, SolidBorder border) {
        Paragraph p = new Paragraph(text).setFontSize(7).setBold().setFontColor(HEADER_FG);
        if (cyrillicFont != null) p.setFont(cyrillicFont);
        return new Cell()
                .add(p)
                .setBackgroundColor(HEADER_BG)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(border)
                .setPadding(2);
    }

    private Cell dataCell(String text, SolidBorder border, DeviceRgb bg) {
        Paragraph p = new Paragraph(text).setFontSize(7);
        if (cyrillicFont != null) p.setFont(cyrillicFont);
        return new Cell()
                .add(p)
                .setBackgroundColor(bg)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(border)
                .setPadding(2);
    }

    private Cell summaryCell(String text) {
        Paragraph p = new Paragraph(text).setFontSize(9).setBold()
                .setFontColor(new DeviceRgb(30, 64, 175));
        if (cyrillicFont != null) p.setFont(cyrillicFont);
        return new Cell()
                .add(p)
                .setBackgroundColor(SUMMARY_BG)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(new SolidBorder(BORDER_CLR, 0.5f))
                .setPadding(6);
    }

    private PdfFont loadCyrillicFont() throws IOException {
        // Try bundled classpath font first (works on any OS)
        try (InputStream is = getClass().getResourceAsStream("/fonts/arial.ttf")) {
            if (is != null) {
                byte[] fontBytes = is.readAllBytes();
                log.debug("Загружен шрифт из classpath: /fonts/arial.ttf");
                return PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            }
        }
        // Fallback: system paths (Linux/Docker)
        String[] paths = {
            cyrillicFontPath,
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/TTF/DejaVuSans.ttf",
            "/usr/share/fonts/dejavu-sans/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/freefont/FreeSans.ttf"
        };
        for (String path : paths) {
            if (new File(path).exists()) {
                log.debug("Загружен шрифт для отчёта: {}", path);
                return PdfFontFactory.createFont(path, PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            }
        }
        log.error("Не найден шрифт с кириллицей для attendance-отчёта!");
        return null;
    }

    private Paragraph styledParagraph(String text, float size, boolean bold, DeviceRgb color) {
        Paragraph p = new Paragraph(text).setFontSize(size).setFontColor(color);
        if (bold) p.setBold();
        if (cyrillicFont != null) p.setFont(cyrillicFont);
        return p;
    }
}

