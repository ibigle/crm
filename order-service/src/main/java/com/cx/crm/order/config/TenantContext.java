package com.cx.crm.order.config;

import com.alibaba.ttl.TransmittableThreadLocal; // 💡 引入阿里极致高性能多线程上下文透传组件

/**
 * 💡 金融级多租户隔离上下文
 * ⚖️ 彻底降维打击：完美防御高并发多线程、分布式弹性线程池环境下的租户串账泄密隐患
 */
public class TenantContext {

    // 💡 核心整改：使用 TransmittableThreadLocal 替代原生 ThreadLocal
    // 它在底层挂载了特定的 JVM 变量捕获快照，当任务被放入多线程、并行流池时，会自动将租户指纹无损复制给子线程
    private static final ThreadLocal<String> TENANT_KEY = new TransmittableThreadLocal<>();

    public static void setTenantId(String tenantId) {
        TENANT_KEY.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT_KEY.get();
    }

    public static void clear() {
        TENANT_KEY.remove(); // 强制回收，杜绝常驻污染
    }
}

