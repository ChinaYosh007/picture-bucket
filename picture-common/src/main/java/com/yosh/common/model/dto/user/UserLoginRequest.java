package com.yosh.common.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录：邮箱或账号 + 密码 + 邮箱验证码。
 */
@Data
public class UserLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 邮箱或用户账号（二选一填写同一字段）。
     */
    private String account;

    /**
     * 密码。
     */
    private String userPassword;

    /**
     * 邮箱验证码（发到绑定邮箱）。
     */
    private String emailCode;
}
