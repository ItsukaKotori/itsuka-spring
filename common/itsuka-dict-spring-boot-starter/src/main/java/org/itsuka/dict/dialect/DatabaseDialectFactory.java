package org.itsuka.dict.dialect;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据库方言工厂，用于创建对应数据库类型的方言实例
 */
@Slf4j
public class DatabaseDialectFactory {
    
    /**
     * 根据驱动类名获取对应的数据库方言
     * @param driverClassName 数据库驱动类名
     * @return 对应数据库类型的方言实例
     */
    public static DatabaseDialect getDialect(String driverClassName) {
        if (driverClassName == null || driverClassName.isEmpty()) {
            log.warn("数据库驱动类名为空，使用MySQL方言作为默认值");
            return new MySQLDialect();
        }
        
        driverClassName = driverClassName.toLowerCase();
        
        if (driverClassName.contains("mysql")) {
            return new MySQLDialect();
        } else if (driverClassName.contains("oracle")) {
            return new OracleDialect();
        } else if (driverClassName.contains("postgresql")) {
            return new PostgreSQLDialect();
        } else {
            // 默认返回MySQL方言
            log.warn("未识别的数据库类型: {}, 使用MySQL方言作为默认值", driverClassName);
            return new MySQLDialect();
        }
    }
    
    /**
     * 根据数据库URL获取对应的数据库方言
     * @param url 数据库连接URL
     * @return 对应数据库类型的方言实例
     */
    public static DatabaseDialect getDialectByUrl(String url) {
        if (url == null || url.isEmpty()) {
            log.warn("数据库URL为空，使用MySQL方言作为默认值");
            return new MySQLDialect();
        }
        
        url = url.toLowerCase();
        
        if (url.startsWith("jdbc:mysql:")) {
            return new MySQLDialect();
        } else if (url.startsWith("jdbc:oracle:")) {
            return new OracleDialect();
        } else if (url.startsWith("jdbc:postgresql:")) {
            return new PostgreSQLDialect();
        } else {
            // 默认返回MySQL方言
            log.warn("未识别的数据库类型: {}, 使用MySQL方言作为默认值", url);
            return new MySQLDialect();
        }
    }
}