package kz.whosnext.hr.documentservice.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class QrCodeService {

    @Value("${app.base-url:https://hr.yourcompany.kz}")
    private String baseUrl;

    public byte[] generateQrCode(UUID documentId, int width, int height) {
        return generateQrCodeFromText(getVerifyUrl(documentId), width, height);
    }

    public byte[] generateVerificationQrCode(UUID documentId, int width, int height) {
        return generateQrCodeFromText(getVerifyUrl(documentId), width, height);
    }

    public byte[] generateSignatureQrCode(UUID documentId, int width, int height) {
        return generateQrCodeFromText(getSignatureUrl(documentId), width, height);
    }

    public byte[] generateQrCodeFromText(String content, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.CHARACTER_SET, "UTF-8",
                    EncodeHintType.MARGIN, 1
            );
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            log.debug("QR-код сгенерирован для payload длиной {}", content.length());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Ошибка генерации QR-кода: {}", e.getMessage());
            throw new RuntimeException("Ошибка генерации QR-кода", e);
        }
    }

    public String getVerifyUrl(UUID documentId) {
        return baseUrl + "/api/v1/contracts/" + documentId + "/verify";
    }

    public String getSignatureUrl(UUID documentId) {
        return baseUrl + "/api/v1/contracts/" + documentId + "/signature";
    }
}
