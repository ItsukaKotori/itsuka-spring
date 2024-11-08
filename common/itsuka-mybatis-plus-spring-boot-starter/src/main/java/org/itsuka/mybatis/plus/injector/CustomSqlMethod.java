package org.itsuka.mybatis.plus.injector;

import lombok.Getter;

@Getter
public enum CustomSqlMethod {
    SELECT_VALUE_RANGE("selectValueRange",
            "查询某个字段的所有值(去重)",
            "<script>%s SELECT distinct(%s) FROM %s %s %s\n</script>");

    private final String method;
    private final String desc;
    private final String sql;

    CustomSqlMethod(String method, String desc, String sql) {
        this.method = method;
        this.desc = desc;
        this.sql = sql;
    }
}
