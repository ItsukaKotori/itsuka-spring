package org.itsuka.log.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

class ToStringLogParameterSerializerTest {

    record Param(String orderNo, int count) {}

    static class SampleService {
        public void update(Param param, int count) {}
    }

    @Test
    void skipsServletAndMultipartArguments() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class, int.class);
        ToStringLogParameterSerializer serializer = new ToStringLogParameterSerializer();

        Param first = new Param("NO1", 2);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockMultipartFile file =
            new MockMultipartFile("file", "x.txt", "text/plain", "data".getBytes());

        String result = serializer.serialize(
            method, new Object[]{first, request, file, new Param("NO2", 3)});
        assertThat(result).contains("NO1").contains("NO2");
        assertThat(result).doesNotContain("MockHttpServletRequest").doesNotContain("MockMultipartFile");
    }

    @Test
    void returnsNullWhenAllArgumentsIgnored() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class, int.class);
        ToStringLogParameterSerializer serializer = new ToStringLogParameterSerializer();
        String result = serializer.serialize(method, new Object[]{
            new MockHttpServletRequest(),
            new MockMultipartFile("f", "x.txt", "text/plain", "d".getBytes())
        });
        assertThat(result).isNull();
    }
}
