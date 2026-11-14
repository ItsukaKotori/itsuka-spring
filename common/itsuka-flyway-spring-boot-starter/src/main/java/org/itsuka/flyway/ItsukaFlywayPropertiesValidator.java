package org.itsuka.flyway;

import org.itsuka.flyway.ItsukaFlywayProperties.Source;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 校验 {@link ItsukaFlywayProperties} 中的 source 配置。
 *
 * <p>在注册 Bean 与执行迁移之前集中失败，避免非法 name、空 location 或历史表名冲突
 * 以更难定位的方式在 Flyway 内部暴露。</p>
 *
 * @author Itsuka
 */
final class ItsukaFlywayPropertiesValidator {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]*$");
    private static final Pattern TABLE_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
    private static final int MAX_TABLE_LENGTH = 64;

    private ItsukaFlywayPropertiesValidator() {
    }

    /**
     * 校验 source 的 name、location、历史表名及其唯一性。
     *
     * @param properties 待校验的配置属性
     * @throws IllegalStateException 配置非法时抛出，消息以具体字段路径开头
     */
    static void validate(ItsukaFlywayProperties properties) {
        List<Source> sources = properties.getSources();
        if (sources.isEmpty()) {
            return;
        }

        Set<String> normalizedNames = new HashSet<>();
        for (int i = 0; i < sources.size(); i++) {
            Source source = sources.get(i);
            String path = "itsuka.flyway.sources[" + i + "]";
            if (source == null) {
                throw new IllegalStateException(path + " 不能为空");
            }
            String name = source.getName();
            if (name == null || name.isBlank()) {
                throw new IllegalStateException(path + ".name 不能为空");
            }
            if (!NAME_PATTERN.matcher(name).matches()) {
                throw new IllegalStateException(path + ".name 格式非法: " + name
                    + "，必须匹配 " + NAME_PATTERN.pattern());
            }
            String normalizedName = source.normalizedName().toLowerCase(Locale.ROOT);
            if (!normalizedNames.add(normalizedName)) {
                throw new IllegalStateException(path + ".name 规范化后重复: " + name
                    + "（规范化结果 " + normalizedName + "）");
            }
            String location = source.getLocation();
            if (location == null || location.isBlank()) {
                throw new IllegalStateException(path + ".location 不能为空");
            }
        }

        Set<String> tables = new HashSet<>();
        for (int i = 0; i < sources.size(); i++) {
            Source source = sources.get(i);
            String path = "itsuka.flyway.sources[" + i + "]";
            String table = source.resolvedTable();
            if (!TABLE_PATTERN.matcher(table).matches()) {
                throw new IllegalStateException(path + ".table 格式非法: " + table
                    + "，只能包含字母、数字、下划线，且不能以数字开头");
            }
            if (table.length() > MAX_TABLE_LENGTH) {
                throw new IllegalStateException(path + ".table 长度不能超过 " + MAX_TABLE_LENGTH
                    + ": " + table + "，请显式配置更短的 table");
            }
            if (!tables.add(table.toLowerCase(Locale.ROOT))) {
                throw new IllegalStateException(path + ".table 历史表名重复（忽略大小写）: " + table);
            }
        }
    }
}
