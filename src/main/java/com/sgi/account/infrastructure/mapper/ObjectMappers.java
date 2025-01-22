package com.sgi.account.infrastructure.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility class for handling ObjectMapper instances.
 * This class provides a pre-configured ObjectMapper instance with:
 * - JavaTimeModule for handling Java 8 time types.
 * - Disabling the failure on unknown properties during deserialization.
 */
public class ObjectMappers {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
}