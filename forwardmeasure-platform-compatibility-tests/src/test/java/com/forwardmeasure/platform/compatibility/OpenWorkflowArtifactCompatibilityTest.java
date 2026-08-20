/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package com.forwardmeasure.platform.compatibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.forwardmeasure.openworkflow.authorization.AuthorizationAction;
import com.forwardmeasure.openworkflow.definition.management.api.model.CreateWorkflowDefinitionRequest;
import com.forwardmeasure.openworkflow.engine.api.ExecutionStatus;
import com.forwardmeasure.openworkflow.execution.api.model.ExecutionStart;
import org.junit.jupiter.api.Test;

class OpenWorkflowArtifactCompatibilityTest {
  @Test
  void consumesTheUnifiedPublishedOpenWorkflowContracts() {
    assertEquals("COMPLETED", ExecutionStatus.COMPLETED.name());
    assertNotNull(AuthorizationAction.values());
    assertNotNull(new CreateWorkflowDefinitionRequest());
    assertNotNull(new ExecutionStart());
  }
}
