package com.cx.crm.order.filter;

import lombok.Data;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@Configuration
@RefreshScope
public class TenantMetaCache {
    // 💡 租户隔离策略枚举：SCHEMA(独立库) / COLUMN(共享库字段隔离)
    public enum Strategy {SCHEMA, COLUMN}

    // 内存极速指纹路由表（实际生产中通过定时任务从“租户元数据库”异步同步刷新）
    private static final Map<String, Strategy> tenantStrategyMap = new HashMap<>();
    private static final Set<String> globalTables = new HashSet<>();

    static {
        // 租户元数据示例配置
        tenantStrategyMap.put("tenant001", Strategy.SCHEMA); // 租户1：走独立 Schema
        tenantStrategyMap.put("tenant002", Strategy.COLUMN); // 租户2：走字段隔离

        // 💡 全局表声明：无论哪种隔离模式，遇到系统字典表或公共商品类目表，直接绕过多租户拦截器
        globalTables.add("sys_dict");
        globalTables.add("pub_commodity_category");
    }

    public static Strategy getStrategy(String tenantId) {
        return tenantStrategyMap.getOrDefault(tenantId, Strategy.COLUMN);
    }

    public static boolean isGlobalTable(String tableName) {
        return globalTables.contains(tableName.toLowerCase());
    }
}

