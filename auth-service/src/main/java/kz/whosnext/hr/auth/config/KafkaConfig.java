package kz.whosnext.hr.auth.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topics.user-registered}")
    private String userRegisteredTopic;

    @Value("${app.kafka.topics.candidate-promoted}")
    private String candidatePromotedTopic;

    @Value("${app.kafka.topics.document-signed}")
    private String documentSignedTopic;

    @Value("${app.kafka.topics.activity-log}")
    private String activityLogTopic;

    @Value("${app.kafka.dlt.suffix:.dlt}")
    private String dltSuffix;

    @Value("${app.kafka.consumer.retry.backoff-ms:2000}")
    private long retryBackoffMs;

    @Value("${app.kafka.consumer.retry.max-attempts:5}")
    private long maxAttempts;

    @Bean
    @Profile("!test")
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(userRegisteredTopic).partitions(3).replicas(1).build();
    }

    @Bean
    @Profile("!test")
    public NewTopic candidatePromotedTopic() {
        return TopicBuilder.name(candidatePromotedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    @Profile("!test")
    public NewTopic documentSignedTopic() {
        return TopicBuilder.name(documentSignedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    @Profile("!test")
    public NewTopic userRegisteredDltTopic() {
        return TopicBuilder.name(userRegisteredTopic + dltSuffix).partitions(3).replicas(1).build();
    }

    @Bean
    @Profile("!test")
    public NewTopic candidatePromotedDltTopic() {
        return TopicBuilder.name(candidatePromotedTopic + dltSuffix).partitions(3).replicas(1).build();
    }

    @Bean
    @Profile("!test")
    public NewTopic documentSignedDltTopic() {
        return TopicBuilder.name(documentSignedTopic + dltSuffix).partitions(3).replicas(1).build();
    }

    @Bean
    @Profile("!test")
    public NewTopic activityLogTopic() {
        return TopicBuilder.name(activityLogTopic).partitions(3).replicas(1).build();
    }

    @Bean
    @Profile("!test")
    public NewTopic activityLogDltTopic() {
        return TopicBuilder.name(activityLogTopic + dltSuffix).partitions(3).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + dltSuffix, record.partition())
        );
        FixedBackOff backOff = new FixedBackOff(retryBackoffMs, Math.max(0, maxAttempts - 1));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.setCommitRecovered(true);
        return errorHandler;
    }
}

