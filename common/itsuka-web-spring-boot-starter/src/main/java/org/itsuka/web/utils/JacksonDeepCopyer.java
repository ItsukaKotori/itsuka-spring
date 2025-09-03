package org.itsuka.web.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonDeepCopyer {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> T copy(T source) {
        try {
            String json = mapper.writeValueAsString(source);
            return (T) mapper.readValue(json, source.getClass());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
