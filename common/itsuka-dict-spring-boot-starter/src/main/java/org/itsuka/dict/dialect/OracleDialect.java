package org.itsuka.dict.dialect;

/**
 * Oracle数据库方言实现
 */
public class OracleDialect implements DatabaseDialect {
    
    @Override
    public String getTableExistsSql(String tableName) {
        return String.format("""
               SELECT count(*) 
               FROM user_tables 
               WHERE table_name = '%s'
               """, tableName.toUpperCase());
    }
    
    @Override
    public String getQueryDictDataSql() {
        // Oracle数据库表名通常为大写
        return """
               SELECT d.name AS dict_type, 
                      dd.value AS dict_code, 
                      dd.label AS dict_value 
               FROM dict d 
               JOIN dict_detail dd ON d.id = dd.dict_id
               """;
    }
    
    @Override
    public String getDatabaseType() {
        return "oracle";
    }
    
    @Override
    public String getCreateDictTableSql() {
        // Oracle数据库表名通常为大写，主键需要使用序列或其他方式生成
        return """
               create table dict (
                   id          number(19) primary key,
                   name        varchar2(255) constraint dict_name_unq_key unique,
                   description varchar2(255)
               )
               """;
    }
    
    @Override
    public String getCreateDictDetailTableSql() {
        // Oracle数据库表名通常为大写
        return """
               create table dict_detail (
                   id          number(19) primary key,
                   dict_id     number(19),
                   label       varchar2(255),
                   value       varchar2(255),
                   sort        number(10),
                   constraint fk_dict_detail_dict foreign key (dict_id) references dict(id)
               )
               """;
    }
}