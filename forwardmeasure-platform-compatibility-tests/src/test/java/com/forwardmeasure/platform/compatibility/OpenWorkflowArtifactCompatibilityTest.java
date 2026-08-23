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
