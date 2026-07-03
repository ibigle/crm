//package com.cx.crm.order.config;
//
//import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
//import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
//import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
//import com.cx.crm.order.filter.TenantMetaCache;
//import net.sf.jsqlparser.expression.Expression;
//import net.sf.jsqlparser.expression.StringValue;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * 💡 金融级纵深防御：MyBatis-Plus 多租户自适应混合隔离核心拦截器
// * ⚖️ 遵循规范：职责高度单一，动态判断 Schema/COLUMN 策略，100% 杜绝多线程环境下的多租户越权漏洞
// */
//@Configuration
//public class MyBatisPlusTenantConfiguration {
//
//    @Bean
//    public MybatisPlusInterceptor mybatisPlusInterceptor() {
//        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
//
//        // 💡 注入高度智能、支持多线程并发安全的字段处理器
//        TenantLineHandler tenantLineHandler = new TenantLineHandler() {
//
//            /**
//             * 💡 1. 动态获取当前高并发线程环境下的真实租户ID
//             * 底层基于 TransmittableThreadLocal，即使是在自定义线程池、CompletableFuture 内部执行
//             * 也能通过 JVM 快照技术无损拷贝获取，返回正确的租户标识
//             */
//            @Override
//            public Expression getTenantId() {
//                String currentTenant = TenantContext.getTenantId();
//                // 如果出现未登录或无租户上下文环境，默认使用 PUBLIC 安全域兜底，防御未授权越权漏洞
//                return new StringValue(currentTenant != null ? currentTenant : "PUBLIC");
//            }
//
//            /**
//             * 💡 2. 规定物理数据库中多租户隔离的字段名称
//             */
//            @Override
//            public String getTenantIdColumn() {
//                return "tenant_id";
//            }
//
//            /**
//             * 💡 3. 核心自适应多租户判定矩阵（核心演进盲点修复）
//             * @param tableName 正在被执行编译的物理表名
//             * @return 返回 true 代表【不拼接租户字段】直接放行；返回 false 代表【在 SQL 尾部自动拼接租户过滤条件】
//             */
//            @Override
//            public boolean ignoreTable(String tableName) {
//                // 🛡️ 盾牌一：判定系统全局公共表。
//                // 无论是 Schema 隔离还是字段隔离，系统字典、公共常数表 100% 放行不加字段条件
//                if (TenantMetaCache.isGlobalTable(tableName)) {
//                    return true;
//                }
//
//                // 🛡️ 盾牌二：根据当前请求所绑定的多租户元数据隔离策略进行“弹性降维拦截”
//                String currentTenant = TenantContext.getTenantId();
//                TenantMetaCache.Strategy currentStrategy = TenantMetaCache.getStrategy(currentTenant);
//
//                // 如果元数据配置当前租户为 SCHEMA（独立库隔离），则当前的【字段拦截器】必须在此处失效（返回 true）
//                // 因为它有自己专属的物理连接池隔离，不需要在 SQL 层面污染表结构，从而完美实现混合模式共存！
//                if (currentStrategy == TenantMetaCache.Strategy.SCHEMA) {
//                    return true;
//                }
//
//                // 只有当当前租户属于 COLUMN（共享库字段隔离）模式时，此处返回 false，强制触发底层框架拼装 SQL 条件
//                return false;
//            }
//        };
//
//        // 将装配好的自适应多租户插件挂载到 MyBatis-Plus 拦截器执行链条中
//        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
//
//        return interceptor;
//    }
//}