package com.cx.crm.order.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        DynamicDataSourceRouter router = new DynamicDataSourceRouter();

        // 1. 组装默认的、供字段隔离租户共享的物理数据库连接池
        HikariDataSource defaultSharedDs = new HikariDataSource();
        defaultSharedDs.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/shared_order_db");
        defaultSharedDs.setUsername("root");
        defaultSharedDs.setPassword("password");
        defaultSharedDs.setMaximumPoolSize(100); // 高并发调优：放大连接池

        // 2. 组装专门分配给高级独立库隔离租户 tenant001 的专属物理连接池
        HikariDataSource tenant001Ds = new HikariDataSource();
        tenant001Ds.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/isolated_tenant001_db");
        tenant001Ds.setUsername("tenant001_user");
        tenant001Ds.setPassword("secure_pass");
        tenant001Ds.setMaximumPoolSize(50);

        // 3. 将这组物理连接池注册进动态路由器中进行统一映射接管
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("DEFAULT_COLUMN_SHARED_DB", defaultSharedDs);
        targetDataSources.put("tenant001", tenant001Ds); // Key 必须和租户 ID 严格全等

        router.setTargetDataSources(targetDataSources);
        router.setDefaultTargetDataSource(defaultSharedDs); // 设置默认兜底源

        return router;
    }
}

