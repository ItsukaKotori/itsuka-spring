package org.itsuka.log.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.itsuka.log.properties.LogRecordProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class JacksonLogParameterSerializerTest {

    record Param(String orderNo, int count) {}
    static class SampleService {
        public void update(Param param, int count) {}
    }

    @Test
    void serializesArguments() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class, int.class);
        JacksonLogParameterSerializer serializer =
            new JacksonLogParameterSerializer(JsonMapper.builder().build(), new LogRecordProperties());
        String json = serializer.serialize(method, new Object[]{new Param("NO1", 2), 2});
        assertThat(json).contains("NO1").contains("2");
    }
}
