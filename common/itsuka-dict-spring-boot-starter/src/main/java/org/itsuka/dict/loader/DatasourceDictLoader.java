package org.itsuka.dict.loader;

import org.itsuka.dict.cache.DictCacheManager;
import org.itsuka.dict.properties.DictProperties;
import org.itsuka.dict.properties.DictProperties.DatasourceProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

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
        // 检查表是否存在的SQL
        String checkDictTableSql = "SELECT count(*) FROM information_schema.tables WHERE table_name = 'dict'";
        String checkDictDetailTableSql = "SELECT count(*) FROM information_schema.tables WHERE table_name = 'dict_detail'";
        
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
        
        // 如果表不存在，读取并执行建表SQL
        if (!dictTableExists || !dictDetailTableExists) {
            String createTableSql = readCreateTableSql();
            
            try (Statement stmt = connection.createStatement()) {
                // 分割SQL语句并执行
                String[] sqlStatements = createTableSql.split(";\n");
                for (String sql : sqlStatements) {
                    if (!sql.trim().isEmpty()) {
                        stmt.executeUpdate(sql);
                        log.info("执行SQL: {}", sql);
                    }
                }
                log.info("成功创建字典表");
            }
        } else {
            log.info("字典表已存在，无需创建");
        }
    }
    
    /**
     * 读取建表SQL文件
     */
    private String readCreateTableSql() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/schema/dict.sql");
             Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            
            if (inputStream == null) {
                throw new Exception("未找到dict.sql文件");
            }
            
            // 读取完整的SQL内容
            StringBuilder sqlBuilder = new StringBuilder();
            while (scanner.hasNextLine()) {
                sqlBuilder.append(scanner.nextLine()).append("\n");
            }
            
            return sqlBuilder.toString();
        }
    }

    /**
     * 查询字典数据
     * 从dict和dict_detail表中关联查询字典数据
     */
    private List<Map<String, Object>> queryDictData(Connection connection) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        // 关联查询字典数据
        String sql = "SELECT d.name AS dict_type, dd.value AS dict_code, dd.label AS dict_value " +
                     "FROM dict d JOIN dict_detail dd ON d.id = dd.dict_id";
        
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
