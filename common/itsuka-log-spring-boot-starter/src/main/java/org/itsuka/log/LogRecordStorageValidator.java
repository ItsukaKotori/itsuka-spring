package org.itsuka.log;

import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.spi.LogRecordStore;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Validates that a {@link LogRecordStore} is available for the configured storage before the
 * {@link org.itsuka.log.core.LogRecordAspect} is created.
 *
 * <p>Failing fast with a precise message avoids a confusing {@code NoSuchBeanDefinitionException}
 * later and points the integrator at the two ways to provide a store.</p>
 */
public final class LogRecordStorageValidator {

    private LogRecordStorageValidator() {
    }

    /**
     * Returns the single available {@link LogRecordStore}, or throws when none is registered.
     *
     * @param storeProvider the candidate stores
     * @param properties    the operation log properties, used to report the configured storage
     * @return the store backing the aspect
     * @throws IllegalStateException when no {@link LogRecordStore} bean is available
     */
    public static LogRecordStore require(ObjectProvider<LogRecordStore> storeProvider,
            LogRecordProperties properties) {
        LogRecordStore store = storeProvider.getIfAvailable();
        if (store == null) {
            throw new IllegalStateException(
                    "No LogRecordStore bean found for storage [" + properties.getStorage() + "]. "
                            + "Provide a DataSource through spring-boot-starter-jdbc "
                            + "(itsuka.log.storage=jdbc) or set itsuka.log.storage=slf4j.");
        }
        return store;
    }
}
