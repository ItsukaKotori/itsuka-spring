package org.itsuka.log.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import org.itsuka.log.properties.LogRecordProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.json.JsonMapper;

class JacksonLogParameterSerializerTest {

    record Param(String orderNo, int count) {}
    static class SampleService {
        public void update(Param param, int count) {}
    }
    static class SelfRef {
        private SelfRef self;
        public SelfRef getSelf() {
            return self;
        }
        public void setSelf(SelfRef self) {
            this.self = self;
        }
        @Override
        public String toString() {
            return "SELF";
        }
    }

    @Test
    void serializesArguments() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class, int.class);
        JacksonLogParameterSerializer serializer =
            new JacksonLogParameterSerializer(JsonMapper.builder().build(), new LogRecordProperties());
        String json = serializer.serialize(method, new Object[]{new Param("NO1", 2), 2});
        assertThat(json).contains("NO1").contains("2");
    }

    @Test
    void skipsServletAndMultipartArguments() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class, int.class);
        JsonMapper mapper = JsonMapper.builder().build();
        JacksonLogParameterSerializer serializer =
            new JacksonLogParameterSerializer(mapper, new LogRecordProperties());

        Param first = new Param("NO1", 2);
        Param second = new Param("NO2", 3);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockMultipartFile file = new MockMultipartFile("file", "x.txt", "text/plain", "data".getBytes());

        String json = serializer.serialize(method, new Object[]{first, request, file, second});
        assertThat(json).isEqualTo(mapper.writeValueAsString(List.of(first, second)));
    }

    @Test
    void fallsBackToFilteredDeepToString() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class, int.class);
        JacksonLogParameterSerializer serializer =
            new JacksonLogParameterSerializer(JsonMapper.builder().build(), new LogRecordProperties());

        SelfRef node = new SelfRef();
        node.self = node;

        String json = serializer.serialize(method, new Object[]{node, new MockHttpServletRequest()});
        assertThat(json).contains("SELF");
        assertThat(json).doesNotContain("MockHttpServletRequest");
    }

    @Test
    void truncatesToParamsMaxLength() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class, int.class);
        LogRecordProperties props = new LogRecordProperties();
        props.getRequest().setParamsMaxLength(10);
        JacksonLogParameterSerializer serializer =
            new JacksonLogParameterSerializer(JsonMapper.builder().build(), props);

        String json = serializer.serialize(method, new Object[]{new Param("A".repeat(100), 2)});
        assertThat(json).hasSize(10).endsWith("...");
    }
}
