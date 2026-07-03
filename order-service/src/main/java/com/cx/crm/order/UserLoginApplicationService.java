package com.cx.crm.order;

import com.cx.crm.order.controller.LoginResultVo;

public interface UserLoginApplicationService {
    LoginResultVo authenticateAndIssueReferenceToken(String username, String rawPassword);
}
