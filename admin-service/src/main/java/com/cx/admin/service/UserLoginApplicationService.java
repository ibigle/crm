package com.cx.admin.service;

import com.cx.admin.entity.User;
import com.cx.admin.vo.LoginResultVo;

import java.util.List;

public interface UserLoginApplicationService {
    LoginResultVo authenticateAndIssueReferenceToken(String username, String rawPassword);

    List<User> findUserList();
}

