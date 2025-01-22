package com.sgi.account.infrastructure.subscriber.listener;

import com.sgi.account.application.service.EventHandleService;
import com.sgi.account.infrastructure.annotations.KafkaController;
import com.sgi.account.infrastructure.subscriber.events.BankAccountEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Map;

/**
 * Listener for Kafka topics related to account validation.
 * This class listens to the "validation-request-topic" and processes validation requests.
 */
@KafkaController
@RequiredArgsConstructor
public class TopicListenerAccount {

    private final EventHandleService eventHandleService;

    @KafkaListener(topics = BankAccountEvent.TOPIC,  groupId = "${app.name}")
    @SneakyThrows
    public void handleValidateAccount(BankAccountEvent event) {
        eventHandleService.validateExistBankAccount(event.getBootcoinId(), event.getAccountId());
    }

}
