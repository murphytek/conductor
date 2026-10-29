/*
 * Copyright 2026 Conductor Authors.
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

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.kafkaeq.eventqueue.KafkaObservableQueue;
import com.netflix.conductor.model.TaskModel;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class KafkaTaskStatusPublisherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KafkaObservableQueue queue;

    @Before
    public void setUp() {
        queue = mock(KafkaObservableQueue.class);
    }

    @Test
    public void smallEventCarriesTaskData() throws Exception {
        publisher().onTaskCompleted(task(Map.of("answer", "ok")));

        Map<String, Object> event = published();
        assertEquals("task-1", event.get("taskId"));
        assertEquals("wf-1", event.get("workflowInstanceId"));
        assertEquals("COMPLETED", event.get("status"));
        assertEquals(Map.of("prompt", "p"), event.get("inputData"));
        assertEquals(Map.of("answer", "ok"), event.get("outputData"));
        assertFalse(event.containsKey("payloadTruncated"));
    }

    @Test
    public void secretsInTaskDataAreMasked() throws Exception {
        TaskModel task = task(Map.of("answer", "ok"));
        task.setInputData(
                Map.of(
                        "prompt",
                        "p",
                        "environment",
                        Map.of("ANTHROPIC_API_KEY", "sk-secret", "MODEL", "opus")));
        publisher().onTaskCompleted(task);

        Map<String, Object> event = published();
        assertEquals(
                Map.of(
                        "prompt",
                        "p",
                        "environment",
                        Map.of("ANTHROPIC_API_KEY", PayloadRedactor.REDACTED, "MODEL", "opus")),
                event.get("inputData"));
        assertEquals(Map.of("answer", "ok"), event.get("outputData"));
    }

    @Test
    public void oversizedEventIsSentWithoutTaskData() throws Exception {
        publisher().onTaskCompleted(task(Map.of("answer", "x".repeat(5000))));

        Map<String, Object> event = published();
        assertEquals("task-1", event.get("taskId"));
        assertEquals("COMPLETED", event.get("status"));
        assertFalse(event.containsKey("inputData"));
        assertFalse(event.containsKey("outputData"));
        assertEquals(Boolean.TRUE, event.get("payloadTruncated"));
    }

    private KafkaTaskStatusPublisher publisher() {
        return new KafkaTaskStatusPublisher(
                queue,
                objectMapper,
                1000,
                new PayloadRedactor(PayloadRedactor.DEFAULT_KEY_PATTERN, objectMapper));
    }

    private TaskModel task(Map<String, Object> output) {
        TaskModel task = new TaskModel();
        task.setTaskId("task-1");
        task.setWorkflowInstanceId("wf-1");
        task.setTaskDefName("ai.execute");
        task.setTaskType("SIMPLE");
        task.setReferenceTaskName("research");
        task.setStatus(TaskModel.Status.COMPLETED);
        task.setInputData(Map.of("prompt", "p"));
        task.setOutputData(output);
        return task;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> published() throws Exception {
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(queue).publish(captor.capture());
        return objectMapper.readValue(captor.getValue().get(0).getPayload(), Map.class);
    }
}
