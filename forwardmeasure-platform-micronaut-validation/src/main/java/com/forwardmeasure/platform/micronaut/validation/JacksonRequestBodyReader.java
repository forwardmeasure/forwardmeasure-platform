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
package com.forwardmeasure.platform.micronaut.validation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.Headers;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.body.MessageBodyReader;
import io.micronaut.http.codec.CodecException;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Reads JSON request bodies with Jackson, configured as Quarkus's and Spring Boot's server mappers
 * are (unknown properties ignored; Java time, Optional and parameter-name support), so a body binds
 * - or fails to - exactly as it does there. Micronaut Serde, the reader this outranks, coerces
 * values Jackson rejects (a boolean into a number, for one) and reports failures in its own terms;
 * the contract's models are Jackson-annotated for every framework anyway.
 *
 * <p>Declared over a type variable, as Micronaut's own JSON handler is: Micronaut keeps a reader
 * for a body only if its declared type argument matches the body's type, and a concrete {@code
 * MessageBodyReader<Object>} matches none.
 *
 * <p>A failure surfaces as the body argument's conversion error, with the Jackson exception as its
 * cause ({@link MicronautConversionErrorProblemHandler}). An empty body reads as absent, as on
 * Quarkus and Jersey, so it's reported as a missing body.
 */
@Singleton
@Order(-100) // ahead of Micronaut's own JSON reader, NettyJsonHandler at @Order(-10)
@Consumes({MediaType.APPLICATION_JSON, "text/json"})
public class JacksonRequestBodyReader<T> implements MessageBodyReader<T> {
  private final ObjectMapper mapper =
      JsonMapper.builder()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .addModule(new Jdk8Module())
          .addModule(new JavaTimeModule())
          .addModule(new ParameterNamesModule())
          .build();

  @Override
  public boolean isReadable(Argument<T> type, MediaType mediaType) {
    Class<?> raw = type.getType();
    return !(CharSequence.class.isAssignableFrom(raw)
        || raw == byte[].class
        || InputStream.class.isAssignableFrom(raw)
        || Publisher.class.isAssignableFrom(raw)
        || CompletionStage.class.isAssignableFrom(raw)
        || raw.getName().startsWith("io.micronaut.")
        || raw.getName().startsWith("java.nio."));
  }

  @Override
  public T read(Argument<T> type, MediaType mediaType, Headers headers, InputStream input)
      throws CodecException {
    try (JsonParser parser = mapper.createParser(input)) {
      if (parser.nextToken() == null) return null;
      return mapper.readerFor(mapper.constructType(type.asType())).readValue(parser);
    } catch (IOException e) {
      throw new CodecException(
          "Error decoding JSON body for type [" + type + "]: " + e.getMessage(), e);
    }
  }
}
