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
package com.forwardmeasure.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.forwardmeasure.platform.api.model.EntityKind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The generated EntityKind is the one vocabulary; contracts that list kinds must match it. */
class EntityKindContractTest {

  @Test
  void resolvedEntityRecordContractListsEveryConcreteKind() throws Exception {
    List<String> expected =
        Arrays.stream(EntityKind.values())
            .filter(kind -> kind != EntityKind.UNKNOWN)
            .map(EntityKind::getValue)
            .toList();
    assertEquals(expected, enumValues(read("/contracts/resolved-entity-record-schema-v1.yaml")));
  }

  @Test
  void valuesRoundTripThroughJson() throws Exception {
    var json = new ObjectMapper();
    for (EntityKind kind : EntityKind.values()) {
      String text = json.writeValueAsString(kind);
      assertEquals('"' + kind.getValue() + '"', text);
      assertEquals(kind, json.readValue(text, EntityKind.class));
    }
  }

  private static JsonNode read(String resource) throws Exception {
    try (var stream = EntityKindContractTest.class.getResourceAsStream(resource)) {
      assertNotNull(stream, resource);
      return new ObjectMapper(new YAMLFactory()).readTree(stream);
    }
  }

  private static List<String> enumValues(JsonNode contract) {
    List<String> values = new ArrayList<>();
    contract
        .path("properties")
        .path("entity_kind")
        .path("enum")
        .forEach(value -> values.add(value.asText()));
    return values;
  }
}
