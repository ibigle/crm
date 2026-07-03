package com.cx.crm.order;

import com.alicp.jetcache.anno.*;
import com.cx.crm.order.config.TenantContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl {

    /**
     * 💡 终极三级弹性高并发多租户安全查询
     * L1 缓存：方法栈内部原生隔离
     * L2 缓存：localLimit = 10000 触发 JVM 本地内存高级 Caffeine 快速清洗
     * L3 缓存：lettuce 集群远程读取
     * <p>
     * key 表达式中完美植入了当前线程上下文的 TenantId，实现缓存级数据的逻辑气隙隔离，永不串账！
     */
    @Cached(name = "orderCache:",
            key = "T(com.example.order.config.TenantContext).getTenantId() + '_' + #orderNo",
            expire = 300,
            timeUnit = TimeUnit.SECONDS,
            cacheType = CacheType.BOTH, // BOTH 代表开启多级缓存 (L2 JVM本地 + L3 Redis集群)
            localLimit = 10000)
    @CacheRefresh(refresh = 60, stopRefreshAfterLastAccess = 600, timeUnit = TimeUnit.SECONDS) // 自动异步刷新，抵御缓存击穿
    @CachePenetrationProtect
    public String getOrderNoFromDb(String orderNo) {
        System.out.println("💥 [缓存未命中] 正在穿透进入底层多租户数据库中查询逻辑... 当前租户: "
                + TenantContext.getTenantId() + " 正在访问的单号为: " + orderNo);

        // 底层 MyBatis-Plus 自动依据上述拦截器：
        // 如果是 tenant001 自动无感路由进入物理 Schema 进行单表 select
        // 如果是 tenant002 自动在 SQL 尾部无感拼装: WHERE order_no = 'xxx' AND tenant_id = 'tenant002'
        return "ORDER_ENTITY_OF_DATA_" + orderNo;
    }

    /**
     * 当订单发生修改或关闭时，数据产生变动，强制异步全网核销焚毁各级缓存指纹
     */
    @CacheInvalidate(name = "orderCache:", key = "T(com.example.order.config.TenantContext).getTenantId() + '_' + #orderNo")
    public void updateOrder(String orderNo) {
        System.out.println("🧹 [数据变动] 正在物理同步更新数据库... 正在实时全链路蒸发焚毁对应租户的多级缓存指纹！");
    }
}

