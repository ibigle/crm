package com.cx.admin.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.template.QuickConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cx.admin.entity.AuthorizationPolicy;
import com.cx.admin.entity.User;
import com.cx.admin.entity.UserGroupRel;
import com.cx.admin.mapper.AuthorizationPolicyMapper;
import com.cx.admin.mapper.UserGroupRelMapper;
import com.cx.admin.mapper.UserMapper;
import com.cx.admin.service.UserLoginApplicationService;
import com.cx.admin.vo.LoginResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 💡 金融级纵深防御：MyBatis-Plus 规格用户级联鉴权与幽灵不记名令牌签发大定稿
 */
@Service
public class UserLoginApplicationServiceImpl implements UserLoginApplicationService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserGroupRelMapper userGroupRelMapper;

    @Autowired
    private AuthorizationPolicyMapper policyMapper;

    @Autowired
    private CacheManager cacheManager;

    private volatile Cache<String, String> lazyPhantomTokenCache;

    /**
     * 🚀 编程式双重检查锁（DCL）：动态构建 JetCache 两级大坝，生命线死锁 2 小时
     */
    private Cache<String, String> getPhantomTokenCache() {
        if (lazyPhantomTokenCache == null) {
            synchronized (this) {
                if (lazyPhantomTokenCache == null) {
                    QuickConfig qc = QuickConfig.newBuilder("iam:session:")
                            .expire(Duration.ofHours(2))
                            .cacheType(com.alicp.jetcache.anno.CacheType.BOTH)
                            .build();
                    lazyPhantomTokenCache = cacheManager.getOrCreateCache(qc);
                }
            }
        }
        return lazyPhantomTokenCache;
    }

    @Override
    public LoginResultVo authenticateAndIssueReferenceToken(String username, String rawPassword) {
        /* 1. 基于 MyBatis-Plus 极速抓取受众操作人 */
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUserName, username)
                .last("LIMIT 1"));

        if (user == null || user.getUserStatus() != 1) {
            throw new SecurityException("401: Unauthorized identity account matrix defect");
        }

        /* 密码学强哈希碰撞校对 */
        String clientCryptoPassword = SecureUtil.sha256(rawPassword);
        if (!user.getUserPassword().equalsIgnoreCase(clientCryptoPassword)) {
            throw new SecurityException("401: Unauthorized identity or password verification fractured");
        }

        Long userId = user.getUserId();
        Long tenantId = user.getTenantId();

        /* 2. 调用自定义联查拉出有效角色编码序列 */
        List<String> activeRoleCodes = userMapper.selectRoleCodesByUserId(userId);

        /* 3. 抓取所属用户组 */
        List<UserGroupRel> groupRels = userGroupRelMapper.selectList(new LambdaQueryWrapper<UserGroupRel>()
                .eq(UserGroupRel::getUserId, userId));
        List<Long> affiliatedGroupIds = groupRels.stream()
                .map(UserGroupRel::getGroupId)
                .collect(Collectors.toList());

        /* 4. 多态组装大一统多租户数据隔离过滤条件树 */
        Map<String, List<Object>> aggregatedDimensions = new HashMap<>();

        LambdaQueryWrapper<AuthorizationPolicy> policyWrapper = new LambdaQueryWrapper<>();
        policyWrapper.and(wrapper -> {
            wrapper.eq(AuthorizationPolicy::getPrincipalType, "USER").eq(AuthorizationPolicy::getPrincipalId, userId);
            if (!activeRoleCodes.isEmpty()) {
                wrapper.or(w -> w.eq(AuthorizationPolicy::getPrincipalType, "ROLE").in(AuthorizationPolicy::getPermissionCode, activeRoleCodes));
            }
            if (!affiliatedGroupIds.isEmpty()) {
                wrapper.or(w -> w.eq(AuthorizationPolicy::getPrincipalType, "GROUP").in(AuthorizationPolicy::getPrincipalId, affiliatedGroupIds));
            }
        });

        List<AuthorizationPolicy> totalPolicies = policyMapper.selectList(policyWrapper);

        /* 5. 格式中立无损洗涤：反解物理策略 JSON 维度控制块 */
        for (AuthorizationPolicy policy : totalPolicies) {
            String effect = policy.getPolicyEffect();
            String filtersJson = policy.getDimensionFilters();

            if ("ALLOW".equalsIgnoreCase(effect) && StringUtils.hasText(filtersJson)) {
                try {
                    JSONObject jsonObject = JSON.parseObject(filtersJson);
                    for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                        String dimensionCodeKey = entry.getKey();
                        List<?> rawValues = jsonObject.getList(dimensionCodeKey, Object.class);
                        if (rawValues != null) {
                            aggregatedDimensions.computeIfAbsent(dimensionCodeKey, k -> new ArrayList<>()).addAll(rawValues);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        /* ==================== ⚡ 【大一统 Phantom Token 组装与高熵落盘】 ==================== */
        // A. 物理产生 32 字节高熵不记名引用令牌
        String opaqueReferenceToken = "ibigle_" + IdUtil.fastSimpleUUID();

        // B. 彻底绕开 Jackson 加载死锁！利用 Fastjson 极速组装无状态多租户 Claims
        JSONObject sessionMetadataClaims = new JSONObject();
        sessionMetadataClaims.put("userId", String.valueOf(userId));
        sessionMetadataClaims.put("tenantId", String.valueOf(tenantId));
        sessionMetadataClaims.put("roles", activeRoleCodes);
        sessionMetadataClaims.put("dimensions", aggregatedDimensions);
        sessionMetadataClaims.put("cryptoEnabled", "true");   // 传递给网关层 CipherNonceFilter 联动控制
        sessionMetadataClaims.put("compressEnabled", "true");

        // C. 原子化强刷入两级缓存大坝，完美与网关层的 [AuthTenantFilter] 达成像素级闭环咬合
        getPhantomTokenCache().put(opaqueReferenceToken, sessionMetadataClaims.toJSONString());

        return new LoginResultVo(opaqueReferenceToken, 7200L);
    }

    @Override
    public List<User> findUserList() {
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();

        final List<User> users = userMapper.selectList(userWrapper);
        return users;
    }
}

