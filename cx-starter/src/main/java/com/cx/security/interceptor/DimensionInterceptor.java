package com.cx.security.interceptor;

import com.cx.security.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * MyBatis 数据维度拦截器
 * 在 SELECT 语句执行前，从 TenantContext 中获取维度过滤条件，动态拼接到 SQL 中
 */
@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DimensionInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        MetaObject meta = MetaObject.forObject(handler,
                SystemMetaObject.DEFAULT_OBJECT_FACTORY,
                SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY,
                new DefaultReflectorFactory());

        MappedStatement ms = (MappedStatement) meta.getValue("delegate.mappedStatement");

        // 仅拦截 SELECT 语句，且存在维度过滤条件
        if (ms.getSqlCommandType() == SqlCommandType.SELECT && TenantContext.hasDimension()) {
            BoundSql boundSql = handler.getBoundSql();
            String originalSql = boundSql.getSql();
            String newSql = appendDimensionConditions(originalSql, TenantContext.getDimensionFilters());

            if (!originalSql.equals(newSql)) {
                log.debug("SQL 数据维度增强: \n原SQL: {}\n新SQL: {}", originalSql, newSql);
                meta.setValue("delegate.boundSql.sql", newSql);
            }
        }

        return invocation.proceed();
    }

    /**
     * 在 SQL 中追加维度过滤条件
     */
    private String appendDimensionConditions(String sql, Map<String, List<String>> filters) {
        if (filters == null || filters.isEmpty()) {
            return sql;
        }

        StringBuilder whereClause = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
            String column = entry.getKey();
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                // 过滤空值并转义单引号，防止 SQL 注入
                String inClause = values.stream()
                        .filter(v -> v != null && !v.isEmpty())
                        .map(v -> "'" + v.replace("'", "''") + "'")
                        .collect(Collectors.joining(", "));
                whereClause.append(" AND `").append(column).append("` IN (").append(inClause).append(")");
            }
        }

        if (whereClause.length() == 0) {
            return sql;
        }

        String sqlLower = sql.toLowerCase();
        if (sqlLower.contains("where")) {
            // 已有 WHERE 子句，直接追加 AND
            return sql + whereClause;
        } else {
            // 无 WHERE 子句，在 ORDER BY / GROUP BY 之前插入
            int orderIdx = sqlLower.indexOf("order by");
            int groupIdx = sqlLower.indexOf("group by");
            int insertPos = sql.length();
            if (orderIdx > 0) {
                insertPos = orderIdx;
            } else if (groupIdx > 0) {
                insertPos = groupIdx;
            }
            return sql.substring(0, insertPos) + " WHERE 1=1 " + whereClause + " " + sql.substring(insertPos);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可扩展：从配置文件读取参数，如是否启用、是否打印日志等
    }
}
