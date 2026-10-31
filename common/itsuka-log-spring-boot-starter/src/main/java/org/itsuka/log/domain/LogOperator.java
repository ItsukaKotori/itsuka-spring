package org.itsuka.log.domain;

/**
 * 操作人信息。
 *
 * @param id   操作人 ID
 * @param name 操作人名称
 * @author Itsuka
 */
public record LogOperator(String id, String name) {
}
