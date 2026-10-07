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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonFieldNamesTest {
  interface Readable {
    @JsonProperty("wire_value")
    String getValue();
  }

  static class Parent {
    @JsonProperty("inherited_name")
    String inherited;

    public String getInherited() {
      return inherited;
    }
  }

  static class Model<T> extends Parent {
    @JsonProperty String unchanged;

    @JsonProperty("field_name")
    String field;

    List<String> strings;
    Map<String, Integer> values;
    List<String>[] genericArray;
    T[] unresolvedArray;
    T unresolved;
    List<?> wildcard;
    java.util.Optional<String> optional;

    public boolean isEnabled() {
      return true;
    }

    @JsonProperty("getter_name")
    public String getTitle() {
      return "";
    }

    public void setTitle(String title) {}

    @JsonProperty("setter_name")
    public void setOnly(String value) {}

    public String getInvalid(String ignored) {
      return ignored;
    }

    public void setInvalid() {}

    public void setInvalid(String a, String b) {}

    public void unrelated() {}
  }

  @Test
  void findsWireNamesAcrossFieldsGettersSettersAndInheritance() {
    assertEquals("wire_value", JsonFieldNames.of(Readable.class, "value"));
    assertEquals("missing", JsonFieldNames.of(Readable.class, "missing"));
    assertEquals("inherited_name", JsonFieldNames.of(Model.class, "inherited"));
    assertEquals("field_name", JsonFieldNames.of(Model.class, "field"));
    assertEquals("getter_name", JsonFieldNames.of(Model.class, "title"));
    assertEquals("setter_name", JsonFieldNames.of(Model.class, "only"));
    assertEquals("unchanged", JsonFieldNames.of(Model.class, "unchanged"));
    assertEquals("missing", JsonFieldNames.of(Model.class, "missing"));
    assertEquals("invalid", JsonFieldNames.of(Model.class, "invalid"));
    assertEquals("field", JsonFieldNames.of(null, "field"));
    assertNull(JsonFieldNames.of(Model.class, null));
  }

  @Test
  void discoversOnlyReadablePropertyTypes() {
    assertEquals(boolean.class, JsonFieldNames.typeOf(Model.class, "enabled"));
    assertEquals(String.class, JsonFieldNames.typeOf(Model.class, "title"));
    assertEquals(String.class, JsonFieldNames.typeOf(Model.class, "field"));
    assertNull(JsonFieldNames.typeOf(Model.class, "only"));
    assertNull(JsonFieldNames.typeOf(Model.class, "missing"));
    assertNull(JsonFieldNames.typeOf(null, "title"));
    assertNull(JsonFieldNames.typeOf(Model.class, null));
  }

  @Test
  void discoversCollectionMapAndArrayElementsWithoutGuessingUnresolvedTypes() throws Exception {
    assertEquals(String.class, JsonFieldNames.element(type("strings")));
    assertEquals(Integer.class, JsonFieldNames.element(type("values")));
    assertEquals(String.class, JsonFieldNames.element(String[].class));
    assertEquals(List[].class, JsonFieldNames.raw(type("genericArray")));
    assertEquals(List.class, JsonFieldNames.raw(JsonFieldNames.element(type("genericArray"))));
    assertNull(JsonFieldNames.raw(type("unresolvedArray")));
    assertNull(JsonFieldNames.raw(type("unresolved")));
    assertNull(JsonFieldNames.raw(JsonFieldNames.element(type("wildcard"))));
    assertNull(JsonFieldNames.element(null));
    assertNull(JsonFieldNames.element(String.class));
    assertNull(JsonFieldNames.element(List.class));
    assertNull(JsonFieldNames.element(type("optional")));
    assertNull(JsonFieldNames.element(type("unresolved")));
  }

  private static Type type(String name) throws Exception {
    return Model.class.getDeclaredField(name).getGenericType();
  }
}
