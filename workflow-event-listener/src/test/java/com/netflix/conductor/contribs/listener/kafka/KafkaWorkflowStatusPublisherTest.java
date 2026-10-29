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

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.model.WorkflowModel;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.*;

public class KafkaWorkflowStatusPublisherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockProducer<String, String> producer =
            new MockProducer<>(true, new StringSerializer(), new StringSerializer());

    @Test
    public void secretsInWorkflowInputAreMasked() throws Exception {
        WorkflowModel workflow =
                workflow(
                        Map.of(
                                "catalogAuth",
                                Map.of("clientId", "client-1", "clientSecret", "s3cr3t"),
                                "researchEnvironment",
                                Map.of("OPENAI_API_KEY", "sk-abc")));

        publisher(900_000).onWorkflowCompleted(workflow);

        Map<String, Object> event = published();
        assertEquals(
                Map.of(
                        "catalogAuth",
                        Map.of("clientId", "client-1", "clientSecret", PayloadRedactor.REDACTED),
                        "researchEnvironment",
                        Map.of("OPENAI_API_KEY", PayloadRedactor.REDACTED)),
                payload(event).get("input"));
        assertFalse(event.containsKey("payloadTruncated"));
        // The workflow itself keeps its input.
        assertEquals(
                "s3cr3t", ((Map<?, ?>) workflow.getInput().get("catalogAuth")).get("clientSecret"));
    }

    @Test
    public void oversizedEventIsSentWithoutInputAndOutput() throws Exception {
        publisher(1000).onWorkflowCompleted(workflow(Map.of("big", "x".repeat(5000))));

        Map<String, Object> event = published();
        Map<String, Object> payload = payload(event);
        assertEquals("wf-1", payload.get("workflowId"));
        assertEquals("COMPLETED", payload.get("status"));
        assertNull(payload.get("input"));
        assertNull(payload.get("output"));
        assertEquals(Boolean.TRUE, event.get("payloadTruncated"));
    }

    private KafkaWorkflowStatusPublisher publisher(int maxMessageBytes) {
        KafkaWorkflowStatusPublisherProperties properties =
                new KafkaWorkflowStatusPublisherProperties();
        properties.setMaxMessageBytes(maxMessageBytes);
        return new KafkaWorkflowStatusPublisher(properties, objectMapper, producer);
    }

    private WorkflowModel workflow(Map<String, Object> input) {
        WorkflowDef def = new WorkflowDef();
        def.setName("research");
        def.setVersion(1);
        WorkflowModel workflow = new WorkflowModel();
        workflow.setWorkflowDefinition(def);
        workflow.setWorkflowId("wf-1");
        workflow.setStatus(WorkflowModel.Status.COMPLETED);
        workflow.setCreateTime(1L);
        workflow.setInput(new HashMap<>(input));
        workflow.setOutput(new HashMap<>(Map.of("result", "done")));
        return workflow;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> published() throws Exception {
        assertEquals(1, producer.history().size());
        return objectMapper.readValue(producer.history().get(0).value(), Map.class);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> payload(Map<String, Object> event) {
        return (Map<String, Object>) event.get("payload");
    }
}
