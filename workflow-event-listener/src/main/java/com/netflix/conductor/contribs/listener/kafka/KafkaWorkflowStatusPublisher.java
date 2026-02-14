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

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.common.run.WorkflowSummary;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.core.listener.WorkflowStatusListener;
import com.netflix.conductor.kafkaeq.eventqueue.KafkaObservableQueue;
import com.netflix.conductor.model.WorkflowModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Publishes workflow status change events directly to Kafka topics using {@link
 * KafkaObservableQueue}. Unlike {@code ConductorQueueStatusPublisher} (which uses the internal
 * {@code QueueDAO}), this listener writes to Kafka regardless of the configured queue backend.
 */
public class KafkaWorkflowStatusPublisher implements WorkflowStatusListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(KafkaWorkflowStatusPublisher.class);

    private final KafkaObservableQueue kafkaQueue;
    private final ObjectMapper objectMapper;

    public KafkaWorkflowStatusPublisher(
            KafkaObservableQueue kafkaQueue, ObjectMapper objectMapper) {
        this.kafkaQueue = kafkaQueue;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onWorkflowCompleted(WorkflowModel workflow) {
        publish(workflow, "COMPLETED");
    }

    @Override
    public void onWorkflowTerminated(WorkflowModel workflow) {
        publish(workflow, "TERMINATED");
    }

    @Override
    public void onWorkflowFinalized(WorkflowModel workflow) {
        publish(workflow, "FINALIZED");
    }

    private void publish(WorkflowModel workflow, String eventType) {
        try {
            WorkflowSummary summary = new WorkflowSummary(workflow.toWorkflow());
            String json = objectMapper.writeValueAsString(summary);
            Message message = new Message(workflow.getWorkflowId(), json, null);
            kafkaQueue.publish(Collections.singletonList(message));
            LOGGER.info(
                    "Published {} event for workflow {} to Kafka",
                    eventType,
                    workflow.getWorkflowId());
        } catch (JsonProcessingException e) {
            LOGGER.error(
                    "Failed to serialize workflow {} for {} event",
                    workflow.getWorkflowId(),
                    eventType,
                    e);
        } catch (Exception e) {
            LOGGER.error(
                    "Failed to publish {} event for workflow {} to Kafka",
                    eventType,
                    workflow.getWorkflowId(),
                    e);
        }
    }
}
