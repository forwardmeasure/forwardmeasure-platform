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
package com.forwardmeasure.platform.server.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.forwardmeasure.openworkflow.common.model.Problem;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Real Hibernate Validator method validation against a generated-contract-shaped interface. */
class ConstraintViolationExceptionMapperTest {

  /** Shaped like openapi-generator's jaxrs-spec output: every constraint on the interface. */
  interface ContractApi {
    Response update(
        @HeaderParam("If-Match") @NotNull @Pattern(regexp = "^\"[0-9]+\"$") String ifMatch,
        @PathParam("definitionId") UUID definitionId,
        @Valid @NotNull UpdateRequest updateRequest);

    @NotNull
    String describe();

    Response record(@Valid @NotNull ContractModels.Event event);
  }

  public static class UpdateRequest {
    @NotNull public String displayName;
    @Valid public List<Step> steps = List.of();
  }

  public static class Step {
    @NotNull
    @Size(max = 3)
    public String name;

    Step(String name) {
      this.name = name;
    }
  }

  /** Like the hand-written resources: implements the contract, redeclares nothing. */
  static class ContractResource implements ContractApi {
    @Override
    public Response update(String ifMatch, UUID definitionId, UpdateRequest updateRequest) {
      return Response.ok().build();
    }

    @Override
    public String describe() {
      return null;
    }

    @Override
    public Response record(ContractModels.Event event) {
      return Response.ok().build();
    }
  }

  private static ValidatorFactory factory;
  private static Validator validator;
  private final ConstraintViolationExceptionMapper mapper =
      new ConstraintViolationExceptionMapper();

  @BeforeAll
  static void startValidator() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void closeValidator() {
    factory.close();
  }

  @Test
  void headerViolationsNameTheHeaderAndEchoItsValue() throws Exception {
    UpdateRequest body = new UpdateRequest();
    body.displayName = "valid";

    Problem problem = problem(update("W/7", UUID.randomUUID(), body), 400);

    assertEquals(List.of(violation("If-Match", "W/7")), fieldsAndValues(problem));
    assertEquals("Bad Request", problem.getTitle());
    assertEquals("about:blank", problem.getType());
  }

  @Test
  void bodyViolationsNameTheFieldPathWithinTheBody() throws Exception {
    UpdateRequest body = new UpdateRequest();
    body.steps = List.of(new Step("ok"), new Step(null), new Step("toolong"));

    Problem problem = problem(update("\"3\"", UUID.randomUUID(), body), 400);

    assertEquals(
        List.of(
            violation("displayName", null),
            violation("steps[1].name", null),
            violation("steps[2].name", "toolong")),
        fieldsAndValues(problem));
  }

  /** Fields are named as the client writes them in JSON - the models' {@code @JsonProperty}. */
  @Test
  void bodyViolationsUseTheJsonNamesOfTheGeneratedModels() throws Exception {
    ContractModels.Event event =
        new ContractModels.Event(
            UUID.randomUUID(),
            new ContractModels.ExecutionId(null),
            1L,
            ContractModels.Kind.STARTED,
            new java.util.Date());
    event.setSteps(List.of(new ContractModels.Step("a"), new ContractModels.Step(null)));
    Method record = ContractResource.class.getMethod("record", ContractModels.Event.class);

    Problem problem =
        problem(
            validator
                .forExecutables()
                .validateParameters(new ContractResource(), record, new Object[] {event}),
            400);

    assertEquals(
        List.of(violation("executionId.value", null), violation("steps[1].display_name", null)),
        fieldsAndValues(problem));
  }

  @Test
  void aMissingBodyIsNamedBody() throws Exception {
    Problem problem = problem(update("\"3\"", UUID.randomUUID(), null), 400);

    assertEquals(List.of(violation("body", null)), fieldsAndValues(problem));
  }

  @Test
  void anInvalidResponseIsTheServersFaultAndDisclosesNothing() throws Exception {
    Method describe = ContractResource.class.getMethod("describe");
    Set<ConstraintViolation<ContractResource>> violations =
        validator.forExecutables().validateReturnValue(new ContractResource(), describe, null);

    Problem problem = problem(violations, 500);

    assertTrue(problem.getViolations() == null || problem.getViolations().isEmpty());
    assertNull(problem.getDetail());
  }

  private static Set<ConstraintViolation<ContractResource>> update(
      String ifMatch, UUID definitionId, UpdateRequest body) throws Exception {
    Method update =
        ContractResource.class.getMethod("update", String.class, UUID.class, UpdateRequest.class);
    return validator
        .forExecutables()
        .validateParameters(
            new ContractResource(), update, new Object[] {ifMatch, definitionId, body});
  }

  private Problem problem(Set<? extends ConstraintViolation<?>> violations, int status) {
    Response response = mapper.toResponse(new ConstraintViolationException(violations));
    assertEquals(status, response.getStatus());
    assertEquals("application/problem+json", response.getMediaType().toString());
    Problem problem = (Problem) response.getEntity();
    assertEquals(status, problem.getStatus());
    return problem;
  }

  private static List<List<String>> fieldsAndValues(Problem problem) {
    return problem.getViolations().stream()
        .map(v -> Arrays.asList(v.getField(), v.getRejectedValue()))
        .distinct()
        .toList();
  }

  private static List<String> violation(String field, String rejectedValue) {
    return Arrays.asList(field, rejectedValue);
  }
}
