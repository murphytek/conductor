/*
 * Copyright 2025 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.contribs.listener.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.conductor.core.listener.WorkflowStatusListener;
import com.netflix.conductor.kafkaeq.config.KafkaEventQueueProperties;
import com.netflix.conductor.kafkaeq.eventqueue.KafkaObservableQueue;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Activates when {@code conductor.workflow-status-listener.type=kafka_publisher} and {@code
 * conductor.event-queues.kafka.enabled=true}. Publishes workflow completion/termination events to a
 * single Kafka topic.
 */
@Configuration
@ConditionalOnProperty(
        name = "conductor.workflow-status-listener.type",
        havingValue = "kafka_publisher")
public class KafkaWorkflowStatusPublisherConfiguration {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(KafkaWorkflowStatusPublisherConfiguration.class);

    @Bean
    public WorkflowStatusListener kafkaWorkflowStatusListener(
            KafkaEventQueueProperties kafkaProperties,
            ObjectMapper objectMapper,
            @Value("${conductor.workflow-status-listener.kafka.topic:conductor.workflow.status.v1}")
                    String topic) {
        LOGGER.info("Configuring KafkaWorkflowStatusPublisher for topic '{}'", topic);
        KafkaObservableQueue queue =
                new KafkaObservableQueue.Builder(kafkaProperties).build(topic);
        return new KafkaWorkflowStatusPublisher(queue, objectMapper);
    }
}
