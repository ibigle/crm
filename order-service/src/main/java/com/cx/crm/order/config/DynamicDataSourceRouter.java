package com.cx.crm.order.config;

import com.cx.crm.order.filter.TenantMetaCache;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 💡 金融级多租户隔离：最底层动态数据源路由路由器
 * ⚖️ 遵循规范：配合 TransmittableThreadLocal，实现高并发线程池/异步流下的物理连接池无锁化秒级无感割接
 */
public class DynamicDataSourceRouter extends AbstractRoutingDataSource {

    /**
     * 💡 2000万超高并发核心：决定当前数据库连接切换的 LookupKey
     * 响应式/弹性线程池切换时，底层自动通过 TTL 快照跨线程复制租户 ID，在此处精准返回
     */
    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = TenantContext.getTenantId();

        // 1. 防御性校验：如果当前环境没有任何租户上下文，兜底走默认字段隔离共享库连接池
        if (tenantId == null || "PUBLIC".equalsIgnoreCase(tenantId)) {
            return "DEFAULT_COLUMN_SHARED_DB";
        }

        // 2. 💡 智能路由：从高性能内存元数据缓存中检索该租户的隔离策略
        TenantMetaCache.Strategy strategy = TenantMetaCache.getStrategy(tenantId);

        // 3. 如果元数据宣告该租户属于高级 SCHEMA 隔离（独立物理库），则将租户ID作为 Key 返回
        // Spring 会自动从注册的 TargetDataSources 映射表中捞取对应的物理连接池（如 tenant001 的 HikariCP）
        if (strategy == TenantMetaCache.Strategy.SCHEMA) {
            return tenantId;
        }

        // 4. 其余字段隔离租户，统一返回共享物理库连接池，交由 MyBatis-Plus 拦截器去进行 SQL 层面增量隔离
        return "DEFAULT_COLUMN_SHARED_DB";
    }
}


