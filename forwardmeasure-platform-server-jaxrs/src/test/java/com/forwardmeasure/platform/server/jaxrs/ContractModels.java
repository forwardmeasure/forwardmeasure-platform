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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Request models shaped like openapi-generator's output for the contracts (with
 * openworkflow-api-specifications' template overrides): required properties as {@code @JsonCreator}
 * parameters, JSON names that differ from the Java ones, a string-valued enum, and cascading
 * declared on a collection's element type.
 */
final class ContractModels {
  private ContractModels() {}

  static final class Event {
    private final UUID eventId;
    private final ExecutionId executionId;
    private final Long sequence;
    private final Kind type;
    private final Date occurredAt;
    private List<Step> steps = new ArrayList<>();

    @JsonCreator
    Event(
        @JsonProperty(required = true, value = "eventId") UUID eventId,
        @JsonProperty(required = true, value = "executionId") ExecutionId executionId,
        @JsonProperty(required = true, value = "sequence") Long sequence,
        @JsonProperty(required = true, value = "type") Kind type,
        @JsonProperty(required = true, value = "occurredAt") Date occurredAt) {
      this.eventId = eventId;
      this.executionId = executionId;
      this.sequence = sequence;
      this.type = type;
      this.occurredAt = occurredAt;
    }

    @JsonProperty(required = true, value = "eventId")
    @NotNull
    public UUID getEventId() {
      return eventId;
    }

    @JsonProperty(required = true, value = "executionId")
    @NotNull
    @Valid
    public ExecutionId getExecutionId() {
      return executionId;
    }

    @JsonProperty(required = true, value = "sequence")
    @NotNull
    public Long getSequence() {
      return sequence;
    }

    @JsonProperty(required = true, value = "type")
    @NotNull
    public Kind getType() {
      return type;
    }

    @JsonProperty(required = true, value = "occurredAt")
    @NotNull
    public Date getOccurredAt() {
      return occurredAt;
    }

    @JsonProperty("steps")
    public List<@Valid Step> getSteps() {
      return steps;
    }

    @JsonProperty("steps")
    public void setSteps(List<Step> steps) {
      this.steps = steps;
    }
  }

  static final class ExecutionId {
    private final String value;

    @JsonCreator
    ExecutionId(@JsonProperty(required = true, value = "value") String value) {
      this.value = value;
    }

    @JsonProperty(required = true, value = "value")
    @NotNull
    public String getValue() {
      return value;
    }
  }

  static final class Step {
    private final String displayName;

    @JsonCreator
    Step(@JsonProperty(required = true, value = "display_name") String displayName) {
      this.displayName = displayName;
    }

    @JsonProperty(required = true, value = "display_name")
    @NotNull
    public String getDisplayName() {
      return displayName;
    }
  }

  enum Kind {
    STARTED("STARTED"),
    COMPLETED("COMPLETED");

    private final String value;

    Kind(String value) {
      this.value = value;
    }

    @JsonValue
    @Override
    public String toString() {
      return value;
    }

    @JsonCreator
    static Kind fromValue(String value) {
      for (Kind kind : values()) {
        if (kind.value.equals(value)) return kind;
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }
}
