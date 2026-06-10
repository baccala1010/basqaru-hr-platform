package kz.whosnext.hr.documentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentGeneratorService {

    private final MinioService minioService;

    @Value("${app.pdf.cyrillic-font-path:/usr/share/fonts/dejavu/DejaVuSans.ttf}")
    private String cyrillicFontPath;

    public byte[] generateFromTemplate(String bucket, String templateKey, Map<String, String> placeholders) {
        try (InputStream templateStream = minioService.downloadFile(bucket, templateKey)) {
            XWPFDocument doc = new XWPFDocument(templateStream);

            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                replacePlaceholdersInParagraph(paragraph, placeholders);
            }

            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            replacePlaceholdersInParagraph(paragraph, placeholders);
                        }
                    }
                }
            }

            for (XWPFHeader header : doc.getHeaderList()) {
                for (XWPFParagraph paragraph : header.getParagraphs()) {
                    replacePlaceholdersInParagraph(paragraph, placeholders);
                }
            }

            for (XWPFFooter footer : doc.getFooterList()) {
                for (XWPFParagraph paragraph : footer.getParagraphs()) {
                    replacePlaceholdersInParagraph(paragraph, placeholders);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            doc.close();

            log.info("Документ сгенерирован из шаблона: {}", templateKey);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Ошибка генерации документа из шаблона {}: {}", templateKey, e.getMessage());
            throw new RuntimeException("Ошибка генерации документа", e);
        }
    }

    private void replacePlaceholdersInParagraph(XWPFParagraph paragraph, Map<String, String> placeholders) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null) sb.append(text);
        }
        String combined = sb.toString();
        if (combined.isEmpty()) return;

        boolean hasReplacement = false;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            if (combined.contains(entry.getKey())) {
                hasReplacement = true;
                combined = combined.replace(entry.getKey(), entry.getValue());
            }
        }
        if (!hasReplacement) return;

        for (int i = runs.size() - 1; i > 0; i--) {
            paragraph.removeRun(i);
        }
        runs.get(0).setText(combined, 0);
    }

    /**
     * Converts a filled DOCX (byte[]) to PDF with iText7 preserving basic formatting,
     * then appends an ЭЦП (EDS) block at the bottom.
     *
     * @param docxBytes   filled DOCX bytes (from template or generated)
     * @param docId       document UUID for the EDS block
     * @param signerName  organization name shown in EDS block
     * @param signerBin   organization BIN shown in EDS block
     * @param verifyUrl   optional verification URL
     */
    public byte[] generatePdfFromDocxWithEds(byte[] docxBytes, String docId,
                                              String signerName, String signerBin,
                                              String verifyUrl, String companyCity,
                                              byte[] qrCodeBytes) {
        try {
            XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(docxBytes));
            ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();

            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(pdfOut);
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc =
                    new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document =
                    new com.itextpdf.layout.Document(pdfDoc, com.itextpdf.kernel.geom.PageSize.A4);
            // A4 margins: top/bottom ~20mm, left ~30mm, right ~20mm
            document.setMargins(56f, 57f, 45f, 85f);

            com.itextpdf.kernel.font.PdfFont font = loadCyrillicFont();

            // ── Render DOCX paragraphs with formatting ────────────────────────
            for (XWPFParagraph para : docx.getParagraphs()) {
                List<XWPFRun> runs = para.getRuns();

                // Empty paragraph → small vertical gap
                if (runs == null || runs.isEmpty() || para.getText().trim().isEmpty()) {
                    document.add(new com.itextpdf.layout.element.Paragraph(" ")
                            .setFontSize(5f).setMarginTop(0).setMarginBottom(0));
                    continue;
                }

                com.itextpdf.layout.element.Paragraph p =
                        new com.itextpdf.layout.element.Paragraph();
                for (XWPFRun run : runs) {
                    String runText = run.getText(0);
                    if (runText == null || runText.isEmpty()) continue;
                    double sz = run.getFontSizeAsDouble() != null ? run.getFontSizeAsDouble() : 12.0;
                    if (sz <= 0) sz = 12.0;
                    com.itextpdf.layout.element.Text t =
                            new com.itextpdf.layout.element.Text(runText)
                                    .setFont(font)
                                    .setFontSize((float) sz);
                    if (run.isBold())   t.setBold();
                    if (run.isItalic()) t.setItalic();
                    p.add(t);
                }
                applyParaAlignment(p, para);
                float spacingAfterPt = para.getSpacingAfter() > 0
                        ? para.getSpacingAfter() / 20f : 3f;
                p.setMarginBottom(spacingAfterPt);
                if (para.getIndentationFirstLine() > 0)
                    p.setFirstLineIndent(para.getIndentationFirstLine() / 20f);
                document.add(p);
            }

            // ── Render DOCX tables ────────────────────────────────────────────
            for (XWPFTable tbl : docx.getTables()) {
                if (tbl.getRows().isEmpty()) continue;
                int cols = tbl.getRow(0).getTableCells().size();
                com.itextpdf.layout.element.Table pdfTbl =
                        new com.itextpdf.layout.element.Table(cols).useAllAvailableWidth();
                for (XWPFTableRow row : tbl.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        pdfTbl.addCell(new com.itextpdf.layout.element.Cell()
                                .add(new com.itextpdf.layout.element.Paragraph(
                                        cellText != null ? cellText : "")
                                        .setFont(font).setFontSize(11f))
                                .setPadding(3f));
                    }
                }
                document.add(pdfTbl);
            }

            // ── ЭЦП / EDS block ───────────────────────────────────────────────
            appendEdsBlock(document, font, docId, signerName, signerBin, verifyUrl,
                    companyCity != null ? companyCity : "г. Астана", qrCodeBytes);

            document.close();
            docx.close();
            log.info("PDF с ЭЦП сгенерирован: docId={}, размер={} байт", docId, pdfOut.size());
            return pdfOut.toByteArray();
        } catch (Exception e) {
            log.error("Ошибка генерации PDF из DOCX: {}", e.getMessage());
            throw new RuntimeException("Ошибка генерации PDF", e);
        }
    }

    private void applyParaAlignment(com.itextpdf.layout.element.Paragraph p,
                                    XWPFParagraph para) {
        if (para.getAlignment() == null) return;
        switch (para.getAlignment()) {
            case CENTER -> p.setTextAlignment(
                    com.itextpdf.layout.properties.TextAlignment.CENTER);
            case RIGHT  -> p.setTextAlignment(
                    com.itextpdf.layout.properties.TextAlignment.RIGHT);
            case BOTH   -> p.setTextAlignment(
                    com.itextpdf.layout.properties.TextAlignment.JUSTIFIED);
            default     -> {}
        }
    }

    private void appendEdsBlock(com.itextpdf.layout.Document document,
                                 com.itextpdf.kernel.font.PdfFont font,
                                 String docId, String signerName, String signerBin,
                                 String verifyUrl, String companyCity,
                                 byte[] qrCodeBytes) {
        com.itextpdf.kernel.colors.DeviceRgb black   = new com.itextpdf.kernel.colors.DeviceRgb(0, 0, 0);
        com.itextpdf.kernel.colors.DeviceRgb darkGray = new com.itextpdf.kernel.colors.DeviceRgb(50, 50, 50);
        com.itextpdf.kernel.colors.DeviceRgb gray    = new com.itextpdf.kernel.colors.DeviceRgb(100, 100, 100);
        com.itextpdf.kernel.colors.DeviceRgb bgLight = new com.itextpdf.kernel.colors.DeviceRgb(245, 245, 245);
        com.itextpdf.layout.borders.Border thinBorder =
                new com.itextpdf.layout.borders.SolidBorder(black, 0.75f);
        com.itextpdf.layout.borders.Border noBorder =
                com.itextpdf.layout.borders.Border.NO_BORDER;

        // Separator line
        document.add(new com.itextpdf.layout.element.LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f))
                .setStrokeColor(black).setMarginTop(14f).setMarginBottom(5f));

        String signingDt = java.time.format.DateTimeFormatter
                .ofPattern("dd.MM.yyyy HH:mm:ss")
                .format(java.time.LocalDateTime.now()) + " (UTC+5)";

        // Outer table: [info box 75%] [QR code / stamp 25%]
        com.itextpdf.layout.element.Table outer =
                new com.itextpdf.layout.element.Table(
                        com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[]{75, 25}))
                        .useAllAvailableWidth();

        // ── Info box ─────────────────────────────────────────────────────────
        com.itextpdf.layout.element.Table infoBox =
                new com.itextpdf.layout.element.Table(1).useAllAvailableWidth()
                        .setBorder(thinBorder)
                        .setBackgroundColor(bgLight);

        infoBox.addCell(new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph(
                        "ЭЛЕКТРОННЫЙ ДОКУМЕНТ · ЭЦП ДЕЙСТВИТЕЛЬНА")
                        .setFont(font).setFontSize(8f).setBold().setFontColor(black))
                .setBorder(noBorder).setPadding(5f).setPaddingBottom(2f));

        addEdsRow(infoBox, font, black, gray, noBorder, "Подписант:",       signerName + " · БИН " + signerBin);
        addEdsRow(infoBox, font, black, gray, noBorder, "№ документа:",     docId);
        addEdsRow(infoBox, font, black, gray, noBorder, "Дата подписания:", signingDt);
        addEdsRow(infoBox, font, black, gray, noBorder, "Алгоритм:",        "ГОСТ Р 34.10-2015 (ECDSA-256)");

        // Status row
        com.itextpdf.layout.element.Table statusRow =
                new com.itextpdf.layout.element.Table(
                        com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[]{30, 70}))
                        .useAllAvailableWidth();
        statusRow.addCell(new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph("Статус ЭЦП:")
                        .setFont(font).setFontSize(7f).setFontColor(gray))
                .setBorder(noBorder).setPadding(1f).setPaddingLeft(5f));
        statusRow.addCell(new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph("✓  Действительна")
                        .setFont(font).setFontSize(7.5f).setBold().setFontColor(darkGray))
                .setBorder(noBorder).setPadding(1f));
        infoBox.addCell(new com.itextpdf.layout.element.Cell().add(statusRow)
                .setBorder(noBorder).setPaddingLeft(0).setPaddingRight(0)
                .setPaddingTop(1f).setPaddingBottom(1f));

        if (verifyUrl != null && !verifyUrl.isBlank()) {
            addEdsRow(infoBox, font, black, gray, noBorder, "Проверка:", verifyUrl);
        }

        outer.addCell(new com.itextpdf.layout.element.Cell()
                .add(infoBox).setBorder(noBorder).setPaddingRight(6f));

        // ── Right column: QR code or city stamp ──────────────────────────────
        com.itextpdf.layout.element.Cell rightCell =
                new com.itextpdf.layout.element.Cell()
                        .setBorder(thinBorder)
                        .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                        .setPadding(4f);

        if (qrCodeBytes != null && qrCodeBytes.length > 0) {
            try {
                com.itextpdf.io.image.ImageData qrImg =
                        com.itextpdf.io.image.ImageDataFactory.create(qrCodeBytes);
                com.itextpdf.layout.element.Image qr =
                        new com.itextpdf.layout.element.Image(qrImg)
                                .setAutoScale(true)
                                .setHorizontalAlignment(
                                        com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                rightCell.add(qr);
                rightCell.add(new com.itextpdf.layout.element.Paragraph("Скан для проверки")
                        .setFont(font).setFontSize(6.5f).setFontColor(gray)
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            } catch (Exception e) {
                log.warn("Не удалось вставить QR-код в PDF: {}", e.getMessage());
                addCityStamp(rightCell, font, black, signerName, companyCity);
            }
        } else {
            addCityStamp(rightCell, font, black, signerName, companyCity);
        }
        outer.addCell(rightCell);

        document.add(outer);

        // ── Law reference footer ──────────────────────────────────────────────
        document.add(new com.itextpdf.layout.element.Paragraph(
                "Данный документ согласно пункту 1 статьи 7 Закона РК от 7 января 2003 года " +
                "№370-II «Об электронном документе и электронной цифровой подписи» равнозначен " +
                "документу на бумажном носителе.")
                .setFont(font).setFontSize(7f).setFontColor(gray)
                .setMarginTop(5f)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.JUSTIFIED));
    }

    private void addCityStamp(com.itextpdf.layout.element.Cell cell,
                               com.itextpdf.kernel.font.PdfFont font,
                               com.itextpdf.kernel.colors.DeviceRgb black,
                               String signerName, String companyCity) {
        String shortName = signerName.replace("ТОО ", "").replace("«", "").replace("»", "");
        String city = (companyCity != null && !companyCity.isBlank())
                ? companyCity.replace("г.", "").replace("г .", "").trim().toUpperCase()
                : "АСТАНА";
        cell.add(new com.itextpdf.layout.element.Paragraph(
                shortName + "\n\nЭЦП\n\n" + city)
                .setFont(font).setFontSize(7f).setFontColor(black)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
    }

    private void addEdsRow(com.itextpdf.layout.element.Table infoBox,
                            com.itextpdf.kernel.font.PdfFont font,
                            com.itextpdf.kernel.colors.DeviceRgb black,
                            com.itextpdf.kernel.colors.DeviceRgb gray,
                            com.itextpdf.layout.borders.Border noBorder,
                            String label, String value) {
        com.itextpdf.layout.element.Table row =
                new com.itextpdf.layout.element.Table(
                        com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[]{30, 70}))
                        .useAllAvailableWidth();
        row.addCell(new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph(label)
                        .setFont(font).setFontSize(7f).setFontColor(gray))
                .setBorder(noBorder).setPadding(1f).setPaddingLeft(5f));
        row.addCell(new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph(value)
                        .setFont(font).setFontSize(7.5f).setBold()
                        .setFontColor(black))
                .setBorder(noBorder).setPadding(1f));
        infoBox.addCell(new com.itextpdf.layout.element.Cell().add(row)
                .setBorder(noBorder).setPaddingLeft(0).setPaddingRight(0)
                .setPaddingTop(1f).setPaddingBottom(1f));
    }

    /** Legacy method — kept for compatibility. Prefer generatePdfFromDocxWithEds. */
    public byte[] convertDocxToPdf(byte[] docxBytes) {
        return generatePdfFromDocxWithEds(docxBytes, "—", "—", "—", null, "г. Астана", null);
    }

    private com.itextpdf.kernel.font.PdfFont loadCyrillicFont() throws java.io.IOException {
        java.io.File fontFile = new java.io.File(cyrillicFontPath);
        if (fontFile.exists()) {
            log.debug("Используем шрифт с поддержкой кириллицы: {}", cyrillicFontPath);
            return com.itextpdf.kernel.font.PdfFontFactory.createFont(
                    cyrillicFontPath,
                    com.itextpdf.io.font.PdfEncodings.IDENTITY_H,
                    com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            );
        }
        String[] fallbackPaths = {
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
        for (String path : fallbackPaths) {
            if (new java.io.File(path).exists()) {
                log.warn("Основной шрифт не найден ({}), используем запасной: {}", cyrillicFontPath, path);
                return com.itextpdf.kernel.font.PdfFontFactory.createFont(
                        path,
                        com.itextpdf.io.font.PdfEncodings.IDENTITY_H,
                        com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                );
            }
        }
        log.error("ВНИМАНИЕ: Не найден шрифт с поддержкой кириллицы! " +
                  "Убедитесь, что в Docker-образе установлен font-dejavu. " +
                  "Кирилличные символы в PDF могут не отображаться.");
        return com.itextpdf.kernel.font.PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA
        );
    }

    // ── Certificate DOCX generation (from scratch, no template needed) ──────────

    public byte[] generateCertificateDocx(
            String companyName, String companyBin, String companyCity,
            String directorFullName, String certNum, String issueDateStr,
            String employeeFullName, String position, String department,
            String hireDateStr, String verifyUrl) {

        try (XWPFDocument doc = new XWPFDocument()) {

            // ── Page margins (A4) ──────────────────────────────────────────────
            CTSectPr sect = doc.getDocument().getBody().addNewSectPr();
            CTPageSz pgSz = sect.addNewPgSz();
            pgSz.setW(BigInteger.valueOf(11906));
            pgSz.setH(BigInteger.valueOf(16838));
            CTPageMar pgMar = sect.addNewPgMar();
            pgMar.setTop(BigInteger.valueOf(1134));   // ~2 cm
            pgMar.setBottom(BigInteger.valueOf(1134));
            pgMar.setLeft(BigInteger.valueOf(1701));  // ~3 cm
            pgMar.setRight(BigInteger.valueOf(1134));

            // ── Letterhead ─────────────────────────────────────────────────────
            certPara(doc, companyName, 12, true,  ParagraphAlignment.LEFT, 0, 0);
            certPara(doc, "БИН: " + companyBin, 11, false, ParagraphAlignment.LEFT, 0, 0);
            certPara(doc, companyCity + ", ул. Достық, 12, офис 301", 11, false, ParagraphAlignment.LEFT, 0, 0);
            certPara(doc, "Тел.: +7 (7172) 55-44-33", 11, false, ParagraphAlignment.LEFT, 0, 120);

            // Cert number + date (right-aligned)
            certPara(doc, "№ " + certNum, 11, false, ParagraphAlignment.RIGHT, 0, 0);
            certPara(doc, "Дата выдачи: " + issueDateStr, 11, false, ParagraphAlignment.RIGHT, 0, 240);

            // ── Title ──────────────────────────────────────────────────────────
            certPara(doc, "СПРАВКА", 14, true,  ParagraphAlignment.CENTER, 0, 0);
            certPara(doc, "с места работы", 12, false, ParagraphAlignment.CENTER, 0, 240);

            // ── Body ───────────────────────────────────────────────────────────
            String pos  = (position   != null && !position.isBlank())   ? position   : "___________";
            String dept = (department != null && !department.isBlank()) ? ", отдел " + department : "";
            String hire = (hireDateStr != null && !hireDateStr.isBlank()) ? hireDateStr : "___________";

            certPara(doc,
                "Настоящая справка выдана в том, что " + employeeFullName +
                " действительно работает в " + companyName + " на должности " +
                pos + dept + ".",
                12, false, ParagraphAlignment.BOTH, 0, 160);

            certPara(doc,
                "Работник принят(а) на работу " + hire +
                " на основании трудового договора. Вид занятости: основное место работы, полная рабочая неделя (40 часов).",
                12, false, ParagraphAlignment.BOTH, 0, 160);

            certPara(doc, "Справка выдана для предоставления по месту требования.",
                12, false, ParagraphAlignment.BOTH, 0, 480);

            // ── Signature block (borderless table) ─────────────────────────────
            XWPFTable sig = doc.createTable(1, 3);
            removeCertTableBorders(sig);
            sig.getCTTbl().getTblPr().addNewTblW().setW(BigInteger.valueOf(9070));
            sig.getCTTbl().getTblPr().getTblW().setType(STTblWidth.PCT);

            setCertCell(sig, 0, 0, "Директор\n" + certShortName(directorFullName), 12, ParagraphAlignment.LEFT);
            setCertCell(sig, 0, 1, "________________________", 12, ParagraphAlignment.CENTER);
            setCertCell(sig, 0, 2, "М.П.", 12, ParagraphAlignment.RIGHT);

            // ── Verification URL ───────────────────────────────────────────────
            if (verifyUrl != null && !verifyUrl.isBlank()) {
                certPara(doc, "", 10, false, ParagraphAlignment.LEFT, 240, 0);
                certPara(doc, "Для проверки документа: " + verifyUrl, 9, false, ParagraphAlignment.LEFT, 0, 0);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            log.info("DOCX справка сгенерирована, {} байт", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Ошибка генерации DOCX справки: {}", e.getMessage());
            throw new RuntimeException("Ошибка генерации DOCX справки", e);
        }
    }

    private void certPara(XWPFDocument doc, String text, int size, boolean bold,
                           ParagraphAlignment align, int spaceBefore, int spaceAfter) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(align);
        p.setSpacingBefore(spaceBefore);
        p.setSpacingAfter(spaceAfter);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontFamily("Times New Roman");
        r.setFontSize(size);
        r.setBold(bold);
    }

    private void removeCertTableBorders(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        CTTblBorders b = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
        CTBorder none = CTBorder.Factory.newInstance();
        none.setVal(STBorder.NONE);
        b.setTop(none);   b.setBottom(none);
        b.setLeft(none);  b.setRight(none);
        b.setInsideH(none); b.setInsideV(none);
    }

    private void setCertCell(XWPFTable table, int row, int col,
                              String text, int size, ParagraphAlignment align) {
        XWPFTableCell cell = table.getRow(row).getCell(col);
        boolean first = true;
        for (String line : text.split("\n")) {
            XWPFParagraph p = first ? cell.getParagraphArray(0) : cell.addParagraph();
            p.setAlignment(align);
            p.setSpacingBefore(0);
            p.setSpacingAfter(0);
            XWPFRun r = p.createRun();
            r.setText(line);
            r.setFontFamily("Times New Roman");
            r.setFontSize(size);
            first = false;
        }
    }

    private String certShortName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "—";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) sb.append(' ').append(parts[i].charAt(0)).append('.');
        return sb.toString();
    }

    public byte[] embedQrCodeInPdf(byte[] pdfBytes, byte[] qrCodeBytes) {
        return embedQrCodeInPdf(pdfBytes, qrCodeBytes, 450, 50, 80);
    }

    public byte[] embedQrCodeInPdf(byte[] pdfBytes, byte[] qrCodeBytes, float x, float y, float size) {
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            com.itextpdf.kernel.pdf.PdfReader reader = new com.itextpdf.kernel.pdf.PdfReader(new ByteArrayInputStream(pdfBytes));
            com.itextpdf.kernel.pdf.PdfWriter pdfWriter = new com.itextpdf.kernel.pdf.PdfWriter(result);
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(reader, pdfWriter);

            com.itextpdf.kernel.pdf.PdfPage lastPage = pdfDoc.getLastPage();
            com.itextpdf.kernel.pdf.canvas.PdfCanvas canvas = new com.itextpdf.kernel.pdf.canvas.PdfCanvas(lastPage);

            com.itextpdf.io.image.ImageData imageData = com.itextpdf.io.image.ImageDataFactory.create(qrCodeBytes);
            canvas.addImageFittedIntoRectangle(
                    imageData,
                    new com.itextpdf.kernel.geom.Rectangle(x, y, size, size),
                    false
            );
            canvas.release();
            pdfDoc.close();

            return result.toByteArray();
        } catch (Exception e) {
            log.error("Ошибка вставки QR-кода в PDF: {}", e.getMessage());
            throw new RuntimeException("Ошибка вставки QR-кода", e);
        }
    }
}
