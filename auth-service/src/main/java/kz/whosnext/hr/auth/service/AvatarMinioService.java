package kz.whosnext.hr.auth.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarMinioService {

    private final MinioClient minioClient;

    private static final String BUCKET = "hr-avatars";
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    public String uploadAvatar(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new kz.whosnext.hr.auth.exception.BadRequestException("Файл пуст");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new kz.whosnext.hr.auth.exception.BadRequestException("Файл превышает 5 МБ");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new kz.whosnext.hr.auth.exception.BadRequestException(
                    "Допустимые форматы: JPEG, PNG, WebP, GIF. Получен: " + contentType);
        }

        String ext = extractExtension(file.getOriginalFilename(), contentType);
        String objectKey = "avatars/" + userId + "/" + UUID.randomUUID() + ext;

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(BUCKET)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build());
            log.info("Аватар загружен: userId={}, key={}", userId, objectKey);
            return generatePresignedUrl(objectKey);
        } catch (Exception e) {
            log.error("Ошибка загрузки аватара: {}", e.getMessage());
            throw new RuntimeException("Ошибка загрузки файла в хранилище", e);
        }
    }

    public void deleteOldAvatar(String oldPhotoUrl) {
        if (oldPhotoUrl == null || oldPhotoUrl.isBlank()) return;
        try {
            int idx = oldPhotoUrl.indexOf("/hr-avatars/");
            if (idx == -1) return;
            String objectKey = oldPhotoUrl.substring(idx + "/hr-avatars/".length());
            if (objectKey.contains("?")) {
                objectKey = objectKey.substring(0, objectKey.indexOf("?"));
            }
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(BUCKET)
                    .object(objectKey)
                    .build());
            log.info("Старый аватар удалён: key={}", objectKey);
        } catch (Exception e) {
            log.warn("Не удалось удалить старый аватар: {}", e.getMessage());
        }
    }

    public String generatePresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(BUCKET)
                    .object(objectKey)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации URL аватара", e);
        }
    }

    private String extractExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}

