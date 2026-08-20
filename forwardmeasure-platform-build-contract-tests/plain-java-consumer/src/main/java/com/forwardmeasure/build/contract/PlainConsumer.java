package com.forwardmeasure.build.contract;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Plain Java consumer proving that the core BOM supplies Jackson. */
public final class PlainConsumer {
  private PlainConsumer() {}

  public static String jacksonType() {
    return ObjectMapper.class.getName();
  }
}
