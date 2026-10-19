package com.instagramclone.utility;

import com.instagramclone.common.constants.KafkaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Reusable low-level Kafka publishing helper adhering to Section 49.9 (Kafka Utility).
 * Encapsulates technical event publishing with non-blocking error handling.
 * Does NOT contain domain/business event logic.
 */
@Component
public class KafkaUtility {

    private static final Logger log = LoggerFactory.getLogger(KafkaUtility.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Autowired
    public KafkaUtility(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public boolean isKafkaAvailable() {
        return kafkaEnabled && kafkaTemplate != null;
    }

    public void publish(String topic, String key, Object payload) {
        if (!isKafkaAvailable()) {
            log.debug("Kafka publishing disabled or KafkaTemplate unavailable. Skipping publish to topic '{}' with key '{}'", topic, key);
            return;
        }

        try {
            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Kafka publishing failed for topic '{}' key '{}': {}", topic, key, ex.getMessage());
                        } else {
                            log.debug("Kafka message published to topic '{}' key '{}'", topic, key);
                        }
                    });
        } catch (Exception ex) {
            log.warn("Kafka publish attempt threw exception for topic '{}' key '{}': {}", topic, key, ex.getMessage());
        }
    }

    public void publish(String key, Object payload) {
        publish(KafkaConstants.DEFAULT_TOPIC, key, payload);
    }
}
