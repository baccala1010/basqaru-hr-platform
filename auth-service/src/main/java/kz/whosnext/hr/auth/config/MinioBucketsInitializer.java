package kz.whosnext.hr.auth.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class MinioBucketsInitializer {

    private static final String[] BUCKETS = {"hr-avatars"};

    private final MinioClient minioClient;

    @EventListener(ApplicationReadyEvent.class)
    public void initBuckets() {
        for (String bucket : BUCKETS) {
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Создан бакет MinIO: {}", bucket);
                }
            } catch (Exception e) {
                log.error("Ошибка инициализации бакета MinIO {}: {}", bucket, e.getMessage());
            }
        }
    }
}

