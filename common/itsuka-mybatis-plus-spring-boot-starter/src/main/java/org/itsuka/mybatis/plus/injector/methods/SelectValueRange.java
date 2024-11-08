package org.itsuka.mybatis.plus.injector.methods;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.itsuka.mybatis.plus.injector.CustomSqlMethod;

import static com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils.unSafeParam;
import static org.itsuka.mybatis.plus.Constants.COLUMN;

/**
 * 查询某个字段的所有值(去重)
 */
public class SelectValueRange extends AbstractMethod {

    public SelectValueRange() {
        this(CustomSqlMethod.SELECT_VALUE_RANGE.getMethod());
    }

    /**
     * @param name 方法名
     * @since 3.5.0
     */
    public SelectValueRange(String name) {
        super(name);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        CustomSqlMethod sqlMethod = CustomSqlMethod.SELECT_VALUE_RANGE;
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, String.format(sqlMethod.getSql(),
                sqlFirst(), unSafeParam(COLUMN), tableInfo.getTableName(),
                sqlWhereEntityWrapper(true, tableInfo), sqlComment()), modelClass);
        return this.addSelectMappedStatementForTable(mapperClass, methodName, sqlSource, tableInfo);
    }
}
