package kz.whosnext.hr.documentservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import kz.whosnext.hr.documentservice.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class MinioBucketsInitializer {

    private static final String[] BUCKETS = {"hr-documents", "hr-templates", "hr-uploads"};
    private static final String TEMPLATES_BUCKET = "hr-templates";
    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    /**
     * Pairs of [classpath resource path, MinIO object key].
     * The same resource may appear twice (NDA and PERSONAL_DATA_CONSENT share one template).
     * Keys match what the seed SQL inserts into document_schema.document_templates.
     */
    private static final String[][] TEMPLATE_SEEDS = {
        {"templates/employment_contract.docx",           "templates/employment-contract-template.docx"},
        {"templates/nda_and_personal_data_consent.docx", "templates/nda-template.docx"},
        {"templates/nda_and_personal_data_consent.docx", "templates/personal-data-consent-template.docx"},
        {"templates/work_certificate.docx",              "templates/work_certificate.docx"},
        {"templates/employment_order.docx",              "templates/order-template.docx"},
    };

    private final MinioClient minioClient;
    private final MinioService minioService;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        createBuckets();
        seedTemplates();
    }

    private void createBuckets() {
        for (String bucket : BUCKETS) {
            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Создан бакет MinIO: {}", bucket);
                } else {
                    log.debug("Бакет MinIO уже существует: {}", bucket);
                }
            } catch (Exception e) {
                log.error("Ошибка инициализации бакета MinIO {}: {}", bucket, e.getMessage());
            }
        }
    }

    private void seedTemplates() {
        for (String[] entry : TEMPLATE_SEEDS) {
            String classpathPath = entry[0];
            String minioKey      = entry[1];
            try {
                // Check if already exists in MinIO (try to open the stream)
                if (existsInMinio(minioKey)) {
                    log.debug("Шаблон уже есть в MinIO, пропускаем: {}", minioKey);
                    continue;
                }

                ClassPathResource resource = new ClassPathResource(classpathPath);
                if (!resource.exists()) {
                    log.warn("Шаблон не найден в classpath: {}", classpathPath);
                    continue;
                }

                byte[] bytes;
                try (InputStream is = resource.getInputStream()) {
                    bytes = is.readAllBytes();
                }
                minioService.uploadBytes(TEMPLATES_BUCKET, minioKey, bytes, DOCX_CONTENT_TYPE);
                log.info("Шаблон загружен в MinIO: {} → {} ({} байт)",
                        classpathPath, minioKey, bytes.length);
            } catch (Exception e) {
                log.error("Ошибка загрузки шаблона {} → {}: {}",
                        classpathPath, minioKey, e.getMessage());
            }
        }
    }

    private boolean existsInMinio(String objectKey) {
        try (InputStream is = minioService.downloadFile(TEMPLATES_BUCKET, objectKey)) {
            return is != null;
        } catch (Exception e) {
            return false;
        }
    }
}
