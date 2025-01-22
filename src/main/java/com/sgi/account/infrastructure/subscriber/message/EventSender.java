package com.sgi.account.infrastructure.subscriber.message;

import com.sgi.account.infrastructure.mapper.ObjectMappers;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * EventSender is a component responsible for sending events to Kafka topics.
 * It uses KafkaTemplate to publish events, where the topic name is derived
 * from the class name of the event.
 */
@Component
@Slf4j
public class EventSender {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Constructor for EventSender.
     * Initializes the KafkaTemplate used to send messages to Kafka.
     *
     * @param kafkaTemplate The KafkaTemplate to be used for sending messages.
     */
    public EventSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends an event to a Kafka topic asynchronously.
     * The topic is determined by the class name of the event object.
     *
     * @param event The event to be sent.
     */
    @SneakyThrows
    public void sendEvent(String topic, Object event) {
            String value = ObjectMappers.OBJECT_MAPPER.writeValueAsString(event);
            log.info("Publishing to Kafka topic {}: {}", topic, event);
        kafkaTemplate.send(new ProducerRecord<>(topic, value));
    }
}