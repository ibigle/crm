package com.cx.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Accessors(chain = true)
@Data
@TableName("USER_GROUP_REL_T")
public class UserGroupRel implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "REL_ID", type = IdType.AUTO)
    private Long relId;

    private Long groupId;

    private Long userId;

    private Long tenantId;
}

