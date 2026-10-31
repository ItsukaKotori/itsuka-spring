package org.itsuka.log.store;

import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.spi.LogRecordStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link LogRecordStore} writing operation logs to the dedicated {@code itsuka.operation.log}
 * logger instead of persisting them.
 *
 * <p>The logger is deliberately separate from the application logger so that log routing can
 * send operation logs to a dedicated appender or file.</p>
 */
public class Slf4jLogRecordStore implements LogRecordStore {

    private static final Logger log = LoggerFactory.getLogger("itsuka.operation.log");

    @Override
    public void save(OperationLog operationLog) {
        log.info("operation log bizNo={} type={} category={} success={} operator={} content={} detail={} error={} method={}",
                operationLog.getBizNo(),
                operationLog.getType(),
                operationLog.getCategory(),
                operationLog.isSuccess(),
                operator(operationLog),
                operationLog.getContent(),
                operationLog.getDetail(),
                operationLog.getErrorMsg(),
                operationLog.getMethod());
    }

    private static String operator(OperationLog operationLog) {
        String operatorId = operationLog.getOperatorId();
        String operatorName = operationLog.getOperatorName();
        if (operatorName == null) {
            return operatorId;
        }
        if (operatorId == null) {
            return operatorName;
        }
        return operatorName + "(" + operatorId + ")";
    }
}
