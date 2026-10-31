package org.itsuka.log.spi;

import org.itsuka.log.domain.LogOperator;

/**
 * SPI for resolving the operator of an operation log.
 *
 * <p>Used when the annotation's {@code operator} template is empty.</p>
 */
public interface LogOperatorProvider {

    /**
     * Returns the current operator.
     *
     * @return the operator; implementations should never return {@code null}
     */
    LogOperator getOperator();
}
