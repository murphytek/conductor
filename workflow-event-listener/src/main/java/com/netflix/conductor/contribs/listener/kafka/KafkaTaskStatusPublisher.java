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
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.core.listener.TaskStatusListener;
import com.netflix.conductor.kafkaeq.eventqueue.KafkaObservableQueue;
import com.netflix.conductor.model.TaskModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Publishes task status change events directly to Kafka topics using {@link KafkaObservableQueue}.
 */
public class KafkaTaskStatusPublisher implements TaskStatusListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTaskStatusPublisher.class);

    private final KafkaObservableQueue kafkaQueue;
    private final ObjectMapper objectMapper;

    public KafkaTaskStatusPublisher(KafkaObservableQueue kafkaQueue, ObjectMapper objectMapper) {
        this.kafkaQueue = kafkaQueue;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onTaskScheduled(TaskModel task) {
        publish(task, "SCHEDULED");
    }

    @Override
    public void onTaskInProgress(TaskModel task) {
        publish(task, "IN_PROGRESS");
    }

    @Override
    public void onTaskCanceled(TaskModel task) {
        publish(task, "CANCELED");
    }

    @Override
    public void onTaskFailed(TaskModel task) {
        publish(task, "FAILED");
    }

    @Override
    public void onTaskFailedWithTerminalError(TaskModel task) {
        publish(task, "FAILED_WITH_TERMINAL_ERROR");
    }

    @Override
    public void onTaskCompleted(TaskModel task) {
        publish(task, "COMPLETED");
    }

    @Override
    public void onTaskCompletedWithErrors(TaskModel task) {
        publish(task, "COMPLETED_WITH_ERRORS");
    }

    @Override
    public void onTaskTimedOut(TaskModel task) {
        publish(task, "TIMED_OUT");
    }

    @Override
    public void onTaskSkipped(TaskModel task) {
        publish(task, "SKIPPED");
    }

    private void publish(TaskModel task, String eventType) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("taskId", task.getTaskId());
            payload.put("workflowInstanceId", task.getWorkflowInstanceId());
            payload.put("taskDefName", task.getTaskDefName());
            payload.put("taskType", task.getTaskType());
            payload.put("referenceTaskName", task.getReferenceTaskName());
            payload.put("status", eventType);
            payload.put("retryCount", task.getRetryCount());
            payload.put("seq", task.getSeq());
            payload.put("iteration", task.getIteration());
            payload.put("workerId", task.getWorkerId());
            payload.put("correlationId", task.getCorrelationId());
            payload.put("reasonForIncompletion", task.getReasonForIncompletion());
            payload.put("scheduledTime", task.getScheduledTime());
            payload.put("startTime", task.getStartTime());
            payload.put("updateTime", task.getUpdateTime());
            payload.put("endTime", task.getEndTime());
            payload.put("inputData", task.getInputData());
            payload.put("outputData", task.getOutputData());

            String json = objectMapper.writeValueAsString(payload);
            Message message = new Message(task.getTaskId(), json, null);
            kafkaQueue.publish(Collections.singletonList(message));
            LOGGER.info(
                    "Published {} event for task {} (workflow {}) to Kafka",
                    eventType,
                    task.getTaskId(),
                    task.getWorkflowInstanceId());
        } catch (JsonProcessingException e) {
            LOGGER.error(
                    "Failed to serialize task {} for {} event", task.getTaskId(), eventType, e);
        } catch (Exception e) {
            LOGGER.error(
                    "Failed to publish {} event for task {} to Kafka",
                    eventType,
                    task.getTaskId(),
                    e);
        }
    }
}
