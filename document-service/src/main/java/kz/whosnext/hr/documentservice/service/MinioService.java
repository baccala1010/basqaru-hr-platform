package kz.whosnext.hr.documentservice.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    public String uploadFile(String bucket, String objectKey, MultipartFile file) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("Файл загружен в MinIO: bucket={}, key={}", bucket, objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("Ошибка загрузки файла в MinIO: {}", e.getMessage());
            throw new RuntimeException("Ошибка загрузки файла в MinIO", e);
        }
    }

    public String uploadBytes(String bucket, String objectKey, byte[] data, String contentType) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(bais, data.length, -1)
                    .contentType(contentType)
                    .build());
            log.info("Байты загружены в MinIO: bucket={}, key={}, size={}", bucket, objectKey, data.length);
            return objectKey;
        } catch (Exception e) {
            log.error("Ошибка загрузки байтов в MinIO: {}", e.getMessage());
            throw new RuntimeException("Ошибка загрузки в MinIO", e);
        }
    }

    public InputStream downloadFile(String bucket, String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("Ошибка скачивания файла из MinIO: {}", e.getMessage());
            throw new RuntimeException("Ошибка скачивания файла из MinIO", e);
        }
    }

    public void deleteFile(String bucket, String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            log.info("Файл удалён из MinIO: bucket={}, key={}", bucket, objectKey);
        } catch (Exception e) {
            log.error("Ошибка удаления файла из MinIO: {}", e.getMessage());
            throw new RuntimeException("Ошибка удаления файла из MinIO", e);
        }
    }

    public String generatePresignedUrl(String bucket, String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            log.error("Ошибка генерации presigned URL: {}", e.getMessage());
            throw new RuntimeException("Ошибка генерации URL", e);
        }
    }

    public String generateObjectKey(String prefix, String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return prefix + "/" + UUID.randomUUID() + ext;
    }
}

