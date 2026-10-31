package org.itsuka.log.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.itsuka.log.properties.LogRecordProperties;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link LogParameterSerializer} serializing method arguments to JSON via Jackson 3.
 *
 * <p>Non-serializable arguments ({@link HttpServletRequest}, {@link HttpServletResponse},
 * {@link MultipartFile}) are skipped. When serialization fails the arguments fall back to
 * {@link Arrays#deepToString(Object[])}. The result is truncated to
 * {@code itsuka.log.request.paramsMaxLength}.</p>
 */
public class JacksonLogParameterSerializer implements LogParameterSerializer {

    private final JsonMapper mapper;

    private final LogRecordProperties properties;

    /**
     * Creates a serializer backed by the given mapper and properties.
     *
     * @param mapper     the Jackson 3 JSON mapper
     * @param properties the operation log properties
     */
    public JacksonLogParameterSerializer(JsonMapper mapper, LogRecordProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public String serialize(Method method, Object[] args) {
        if (args == null) {
            return null;
        }
        List<Object> serializable = new ArrayList<>(args.length);
        for (Object arg : args) {
            if (arg == null || isIgnored(arg)) {
                continue;
            }
            serializable.add(arg);
        }
        if (serializable.isEmpty()) {
            return null;
        }
        String result;
        try {
            result = mapper.writeValueAsString(serializable);
        } catch (Exception ex) {
            result = Arrays.deepToString(serializable.toArray());
        }
        return truncate(result);
    }

    private boolean isIgnored(Object arg) {
        return arg instanceof HttpServletRequest
                || arg instanceof HttpServletResponse
                || arg instanceof MultipartFile;
    }

    private String truncate(String value) {
        LogRecordProperties.Request request = properties.getRequest();
        int maxLength = request == null ? 0 : request.getParamsMaxLength();
        return LogRecordTextUtils.truncate(value, maxLength);
    }
}
