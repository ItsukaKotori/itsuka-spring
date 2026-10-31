package org.itsuka.log.query;

import java.util.Optional;

import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.domain.OperationLogPage;
import org.itsuka.log.domain.OperationLogQuery;

/**
 * SPI for querying persisted operation logs.
 *
 * @author Itsuka
 */
public interface OperationLogQueryService {

    /**
     * Queries a page of operation logs matching the given filters.
     *
     * @param query the query conditions, all optional
     * @return the paged result
     */
    OperationLogPage page(OperationLogQuery query);

    /**
     * Looks up a single operation log by its primary key.
     *
     * @param id the primary key
     * @return the matching log, if present
     */
    Optional<OperationLog> findById(Long id);
}
