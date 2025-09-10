package org.itsuka.dict.dialect;

/**
 * 数据库方言接口，用于处理不同数据库的SQL语法差异
 */
public interface DatabaseDialect {
    
    /**
     * 获取检查表是否存在的SQL语句
     * @param tableName 表名
     * @return 检查表是否存在的SQL语句
     */
    String getTableExistsSql(String tableName);
    
    /**
     * 获取查询字典数据的SQL语句
     * @return 查询字典数据的SQL语句
     */
    String getQueryDictDataSql();
    
    /**
     * 获取数据库类型标识
     * @return 数据库类型标识
     */
    String getDatabaseType();
    
    /**
     * 获取创建字典主表的SQL语句
     * @return 创建字典主表的SQL语句
     */
    String getCreateDictTableSql();
    
    /**
     * 获取创建字典详情表的SQL语句
     * @return 创建字典详情表的SQL语句
     */
    String getCreateDictDetailTableSql();
}