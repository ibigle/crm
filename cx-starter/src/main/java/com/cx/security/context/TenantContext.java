package com.cx.security.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.List;
import java.util.Map;

/**
 * 💡 金融级多租户隔离上下文
 * ⚖️ 彻底降维打击：完美防御高并发多线程、分布式弹性线程池环境下的租户串账泄密隐患
 */
public class TenantContext {

    // 使用 TransmittableThreadLocal 替代原生 ThreadLocal
    // 当任务被放入多线程、并行流池时，会自动将租户指纹无损复制给子线程
    private static final ThreadLocal<String> TENANT_KEY = new TransmittableThreadLocal<>();

    /**
     * 当前线程的数据维度过滤条件
     * 示例：{"dept": ["D01", "D02"], "region": ["CN"]}
     * 由 AuthorizationService 从 AuthorizationPolicy 中提取后设置
     */
    private static final ThreadLocal<Map<String, List<String>>> DIMENSION_FILTERS = new TransmittableThreadLocal<>();

    // ============ 数据维度操作 ============

    public static void setDimensionFilters(Map<String, List<String>> filters) {
        DIMENSION_FILTERS.set(filters);
    }

    public static Map<String, List<String>> getDimensionFilters() {
        return DIMENSION_FILTERS.get();
    }

    /**
     * 判断当前线程是否存在维度过滤条件
     */
    public static boolean hasDimension() {
        Map<String, List<String>> filters = DIMENSION_FILTERS.get();
        return filters != null && !filters.isEmpty();
    }

    public static void setTenantId(String tenantId) {
        TENANT_KEY.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT_KEY.get();
    }

    public static void clear() {
        TENANT_KEY.remove(); // 强制回收，杜绝常驻污染
        DIMENSION_FILTERS.remove();
    }
}
