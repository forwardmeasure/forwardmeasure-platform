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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.forwardmeasure.openworkflow.common.model.Problem;
import com.forwardmeasure.openworkflow.common.model.Violation;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.junit.jupiter.api.Test;

class SharedProblemContractTest {
  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void emittedProblemsValidateWithBothNullIncludingAndNullOmittingMappers() throws Exception {
    var problem = new Problem().type("about:blank").title("Bad Request").status(400);
    var schema = schema();
    for (JsonInclude.Include mode :
        new JsonInclude.Include[] {JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_NULL}) {
      var mapper = new ObjectMapper().setSerializationInclusion(mode);
      ObjectNode body = mapper.valueToTree(problem);
      assertTrue(schema.validate(body).isEmpty(), () -> schema.validate(body).toString());
      assertEquals(problem, mapper.treeToValue(body, Problem.class));
    }
    assertEquals(
        java.util.List.of(), problem.getViolations(), "Preserve the existing model default");
  }

  @Test
  void nullableEvidenceIsAcceptedButRequiredFieldsAndTypesRemainEnforced() throws Exception {
    var problem =
        new Problem()
            .type("about:blank")
            .title("Bad Request")
            .status(400)
            .addViolationsItem(
                new Violation()
                    .field("source.uri")
                    .message("must not be blank")
                    .rejectedValue(null));
    ObjectNode body = JSON.valueToTree(problem);
    var schema = schema();
    assertTrue(schema.validate(body).isEmpty(), () -> schema.validate(body).toString());
    assertFalse(schema.validate(body.deepCopy().putNull("status")).isEmpty());
    assertFalse(schema.validate(body.deepCopy().put("status", 600)).isEmpty());
    assertFalse(schema.validate(body.deepCopy().put("status", "400")).isEmpty());
    ObjectNode invalidViolation = body.deepCopy();
    ((ObjectNode) invalidViolation.path("violations").get(0)).remove("message");
    assertFalse(schema.validate(invalidViolation).isEmpty());
    assertFalse(schema.validate(body.deepCopy().put("unrecognized", true)).isEmpty());
  }

  private static Schema schema() throws Exception {
    try (var stream =
        SharedProblemContractTest.class.getResourceAsStream(
            "/META-INF/forwardmeasure/openapi/problem-v1.json")) {
      assertNotNull(stream);
      var document = JSON.readTree(stream);
      ObjectNode root = JSON.createObjectNode().put("$ref", "#/components/schemas/Problem");
      root.set("components", document.required("components"));
      return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(root);
    }
  }
}
