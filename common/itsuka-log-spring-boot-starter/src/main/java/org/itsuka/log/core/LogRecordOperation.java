package org.itsuka.log.core;

/**
 * Parsed {@link org.itsuka.log.annotation.LogRecord} values for one method.
 *
 * @param success   success template
 * @param fail      failure template
 * @param operator  operator expression template
 * @param bizNo     business number expression template
 * @param type      static business type
 * @param category  static business category
 * @param detail    detail expression template
 * @param condition condition SpEL expression
 */
public record LogRecordOperation(
        String success,
        String fail,
        String operator,
        String bizNo,
        String type,
        String category,
        String detail,
        String condition) {
}
