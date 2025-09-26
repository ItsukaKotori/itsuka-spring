package org.itsuka.dict.loader;

import org.itsuka.core.utils.ReflectionKit;
import org.itsuka.dict.cache.DictCacheManager;
import org.itsuka.dict.dialect.DatabaseDialect;
import org.itsuka.dict.dialect.DatabaseDialectFactory;
import org.itsuka.dict.properties.DictProperties;
import org.itsuka.dict.properties.DictProperties.DatasourceProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class DatasourceDictLoader implements DictLoader {

    private final DictProperties properties;
    
    private final DictCacheManager dictCacheManager;

    @Override
    public void load() {
        if (!properties.isAutoLoad()) {
            log.info("未启用自动加载，跳过...");
            return;
        }
        
        DatasourceProperties datasource = properties.getDatasource();
        if (datasource == null) {
            log.warn("数据源配置为空，无法加载字典数据");
            return;
        }

        Connection connection = null;
        try {
            // 1. 加载驱动
            Class.forName(datasource.getDriverClassName());
            
            // 2. 建立数据库连接
            connection = DriverManager.getConnection(
                    datasource.getUrl(), 
                    datasource.getUsername(), 
                    datasource.getPassword()
            );
            
            // 3. 检测并创建字典表
            checkAndCreateDictTables(connection);
            
            // 4. 查询字典数据
            List<Map<String, Object>> dictData = queryDictData(connection);
            
            // 5. 将字典数据放入缓存
            putDictDataToCache(dictData);
            
            log.info("成功加载字典数据，共 {} 条", dictData.size());
            
        } catch (Exception e) {
            log.error("加载字典数据失败", e);
        } finally {
            // 6. 关闭数据库连接
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.error("关闭数据库连接失败", e);
                }
            }
        }
    }

    /**
     * 检测并创建字典表
     */
    private void checkAndCreateDictTables(Connection connection) throws Exception {
        // 获取数据库方言
        DatabaseDialect dialect = getDatabaseDialect();
        log.info("使用数据库方言: {}", dialect.getDatabaseType());
        
        // 检查表是否存在的SQL（使用对应数据库方言的SQL）
        String checkDictTableSql = dialect.getTableExistsSql("dict");
        String checkDictDetailTableSql = dialect.getTableExistsSql("dict_detail");
        
        boolean dictTableExists = false;
        boolean dictDetailTableExists = false;
        
        // 检查dict表是否存在
        try (PreparedStatement ps = connection.prepareStatement(checkDictTableSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                dictTableExists = true;
            }
        }
        
        // 检查dict_detail表是否存在
        try (PreparedStatement ps = connection.prepareStatement(checkDictDetailTableSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                dictDetailTableExists = true;
            }
        }
        
        // 如果表不存在，使用数据库方言提供的SQL创建表
        if (!dictTableExists || !dictDetailTableExists) {
            try (Statement stmt = connection.createStatement()) {
                // 如果字典主表不存在，创建字典主表
                if (!dictTableExists) {
                    String createDictTableSql = dialect.getCreateDictTableSql();
                    stmt.executeUpdate(createDictTableSql);
                    log.info("执行创建字典主表SQL: {}", createDictTableSql);
                }
                
                // 如果字典详情表不存在，创建字典详情表
                if (!dictDetailTableExists) {
                    String createDictDetailTableSql = dialect.getCreateDictDetailTableSql();
                    stmt.executeUpdate(createDictDetailTableSql);
                    log.info("执行创建字典详情表SQL: {}", createDictDetailTableSql);
                }
                
                log.info("成功创建字典表");
            }
        } else {
            log.info("字典表已存在，无需创建");
        }
    }
    
    /**
     * 查询字典数据
     * 从dict和dict_detail表中关联查询字典数据
     */
    private List<Map<String, Object>> queryDictData(Connection connection) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 获取数据库方言并使用其提供的查询SQL
        DatabaseDialect dialect = getDatabaseDialect();
        String sql = dialect.getQueryDictDataSql();
        log.debug("使用查询SQL: {}", sql);
        
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("dict_type", rs.getString("dict_type"));
                row.put("dict_code", rs.getString("dict_code"));
                row.put("dict_value", rs.getString("dict_value"));
                result.add(row);
            }
        }
        
        return result;
    }
    
    /**
     * 获取数据库方言实例
     * @return 数据库方言实例
     */
    private DatabaseDialect getDatabaseDialect() {
        // 优先使用用户自定义的方言类
        if (properties.getDialectClass() != null && !properties.getDialectClass().trim().isEmpty()) {
            try {
                // 使用 ReflectionKit 安全获取类
                Class<?> dialectClass = ReflectionKit.safeGetClass(properties.getDialectClass());
                if (dialectClass != null && DatabaseDialect.class.isAssignableFrom(dialectClass)) {
                    // 使用 ReflectionKit 创建实例
                    @SuppressWarnings("unchecked")
                    DatabaseDialect customDialect = ReflectionKit.newInstance((Class<DatabaseDialect>) dialectClass);
                    log.info("使用自定义数据库方言: {}", properties.getDialectClass());
                    return customDialect;
                } else {
                    log.warn("配置的方言类 {} 不是 DatabaseDialect 的实现类", properties.getDialectClass());
                }
            } catch (Exception e) {
                log.warn("实例化自定义方言类失败: {}", properties.getDialectClass(), e);
            }
        }
        
        // 其次使用用户配置的数据库类型
        if (properties.getDatabaseType() != null && !properties.getDatabaseType().isEmpty()) {
            String dbType = properties.getDatabaseType().toLowerCase();
            switch (dbType) {
                case "mysql":
                    return new org.itsuka.dict.dialect.MySQLDialect();
                case "oracle":
                    return new org.itsuka.dict.dialect.OracleDialect();
                case "postgresql":
                    return new org.itsuka.dict.dialect.PostgreSQLDialect();
                default:
                    log.warn("配置的数据库类型 {} 不支持，将根据驱动类名自动识别", dbType);
                    break;
            }
        }
        
        DatasourceProperties datasource = properties.getDatasource();
        if (datasource != null) {
            // 先尝试根据驱动类名识别
            if (datasource.getDriverClassName() != null) {
                return DatabaseDialectFactory.getDialect(datasource.getDriverClassName());
            }
            // 再尝试根据URL识别
            if (datasource.getUrl() != null) {
                return DatabaseDialectFactory.getDialectByUrl(datasource.getUrl());
            }
        }
        
        // 默认返回MySQL方言
        log.warn("无法识别数据库类型，使用MySQL方言作为默认值");
        return new org.itsuka.dict.dialect.MySQLDialect();
    }

    /**
     * 将字典数据放入缓存
     * 缓存键格式: dict_type:dict_code
     */
    private void putDictDataToCache(List<Map<String, Object>> dictData) {
        for (Map<String, Object> dictItem : dictData) {
            String dictType = (String) dictItem.get("dict_type");
            String dictCode = (String) dictItem.get("dict_code");
            String dictValue = (String) dictItem.get("dict_value");
            
            // 构建缓存键并存储
            String cacheKey = dictType + ":" + dictCode;
            dictCacheManager.put(cacheKey, dictValue);
        }
    }
}
