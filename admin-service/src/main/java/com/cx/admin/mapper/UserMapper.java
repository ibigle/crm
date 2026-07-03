package com.cx.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cx.admin.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    /* 🌟 高性能多表联查：级联查出用户具备的所有有效角色编码 */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}

