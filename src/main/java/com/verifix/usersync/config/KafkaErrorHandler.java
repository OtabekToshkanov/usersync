package com.verifix.usersync.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.apache.kafka.clients.consumer.Consumer;

// TODO refactor
@Component
@Slf4j
public class KafkaErrorHandler implements ConsumerAwareListenerErrorHandler {

    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException exception, Consumer<?, ?> consumer) {
        log.error("Error in Kafka listener: {}", exception.getMessage(), exception);

        // Log the message that caused the error
        log.error("Problematic message: {}", message.getPayload());

        // Here you could implement additional error handling logic:
        // - Send to dead letter topic
        // - Store in database for manual processing
        // - Send alert notifications

        // For now, we'll just log and return null to continue processing
        return null;
    }
}