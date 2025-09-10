package org.itsuka.dict.dialect;

/**
 * PostgreSQL数据库方言实现
 */
public class PostgreSQLDialect implements DatabaseDialect {
    
    @Override
    public String getTableExistsSql(String tableName) {
        return String.format("""
               SELECT count(*) 
               FROM information_schema.tables 
               WHERE table_name = '%s'
               """, tableName);
    }
    
    @Override
    public String getQueryDictDataSql() {
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
        return "postgresql";
    }
    
    @Override
    public String getCreateDictTableSql() {
        return """
               create table if not exists dict (
                   id          bigint primary key,
                   name        varchar(255) constraint dict_name_unq_key unique,
                   description varchar(255)
               )
               """;
    }
    
    @Override
    public String getCreateDictDetailTableSql() {
        return """
               create table if not exists dict_detail (
                   id          bigint primary key,
                   dict_id     bigint,
                   label       varchar(255),
                   value       varchar(255),
                   sort        integer,
                   constraint fk_dict_detail_dict foreign key (dict_id) references dict(id)
               )
               """;
    }
}