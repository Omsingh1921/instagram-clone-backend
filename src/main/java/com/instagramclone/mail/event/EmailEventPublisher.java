package com.instagramclone.mail.event;

import com.instagramclone.common.constants.KafkaConstants;
import com.instagramclone.utility.KafkaUtility;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes transactional email events strictly AFTER database commit,
 * preventing phantom emails on transaction rollbacks.
 */
@Component
@RequiredArgsConstructor
public class EmailEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EmailEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaUtility kafkaUtility;

    public void publishAfterCommit(EmailEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(event);
                }
            });
        } else {
            dispatch(event);
        }
    }

    private void dispatch(EmailEvent event) {
        log.info("Dispatching email event [ID: {}, Type: {}, Recipient: {}]",
                event.getEventId(), event.getEventType(), event.getEmail());

        // 1. Spring asynchronous in-memory event
        applicationEventPublisher.publishEvent(event);

        // 2. Multi-node Kafka event (when Kafka broker is configured)
        kafkaUtility.publish(KafkaConstants.DEFAULT_TOPIC, event.getEventType(), event);
    }
}
