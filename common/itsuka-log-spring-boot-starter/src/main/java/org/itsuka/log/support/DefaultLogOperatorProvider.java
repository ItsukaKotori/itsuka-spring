package org.itsuka.log.support;

import org.itsuka.log.domain.LogOperator;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.spi.LogOperatorProvider;
import org.springframework.util.StringUtils;

/**
 * {@link LogOperatorProvider} returning the configured default operator.
 *
 * <p>Blank configured values fall back to {@code system} / {@code 系统}.</p>
 */
public class DefaultLogOperatorProvider implements LogOperatorProvider {

    private static final String FALLBACK_ID = "system";

    private static final String FALLBACK_NAME = "系统";

    private final LogRecordProperties properties;

    /**
     * Creates a provider backed by the given properties.
     *
     * @param properties the operation log properties
     */
    public DefaultLogOperatorProvider(LogRecordProperties properties) {
        this.properties = properties;
    }

    @Override
    public LogOperator getOperator() {
        LogRecordProperties.Operator operator = this.properties.getOperator();
        String id = operator == null ? null : operator.getDefaultId();
        String name = operator == null ? null : operator.getDefaultName();
        return new LogOperator(StringUtils.hasText(id) ? id : FALLBACK_ID,
                StringUtils.hasText(name) ? name : FALLBACK_NAME);
    }
}
