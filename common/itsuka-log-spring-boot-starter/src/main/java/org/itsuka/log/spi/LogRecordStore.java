package org.itsuka.log.spi;

import org.itsuka.log.domain.OperationLog;

/**
 * SPI for persisting operation logs.
 *
 * <p>Declaring a custom {@code LogRecordStore} bean replaces the built-in JDBC and SLF4J
 * stores.</p>
 */
public interface LogRecordStore {

    /**
     * Saves the given operation log.
     *
     * @param log the log to persist
     */
    void save(OperationLog log);
}
