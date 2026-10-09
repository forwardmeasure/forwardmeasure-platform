/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at https://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
package com.forwardmeasure.platform.api.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

/** Checks published product snapshots against the authoritative dependency, including dialect. */
public abstract class SharedProblemSchemaContract {
  protected abstract String productDocument();

  @Test
  public final void publishedSharedComponentsMatchTheirAuthoritativeArtifact() throws Exception {
    try (var source = getClass().getResourceAsStream(productDocument());
        var canonical =
            getClass().getResourceAsStream("/META-INF/forwardmeasure/openapi/problem-v1.json")) {
      assertNotNull(source, productDocument());
      assertNotNull(canonical, "Canonical platform contract missing from the dependency artifact");
      var document = new ObjectMapper(new YAMLFactory()).readTree(source);
      var expected = new ObjectMapper().readTree(canonical).path("components").path("schemas");
      var actual = document.path("components").path("schemas");
      for (String name : new String[] {"Problem", "Violation"}) {
        JsonNode snapshot = actual.required(name).deepCopy();
        if (document.path("openapi").asText().startsWith("3.0.")) normalizeNullable(snapshot);
        else rejectLegacyNullable(snapshot);
        assertEquals(
            expected.required(name),
            snapshot,
            "Regenerate shared schemas with"
                + " forwardmeasure-platform/scripts/sync-shared-api-components.py");
      }
    }
  }

  private static void normalizeNullable(JsonNode node) {
    if (node.isObject()) {
      ObjectNode object = (ObjectNode) node;
      if (object.path("nullable").asBoolean()) {
        String type = object.required("type").asText();
        object.remove("nullable");
        object.putArray("type").add(type).add("null");
      }
    }
    for (JsonNode child : node) normalizeNullable(child);
  }

  private static void rejectLegacyNullable(JsonNode node) {
    if (node.isObject()) assertFalse(node.has("nullable"), "OpenAPI 3.1 uses a null type union");
    for (JsonNode child : node) rejectLegacyNullable(child);
  }
}
