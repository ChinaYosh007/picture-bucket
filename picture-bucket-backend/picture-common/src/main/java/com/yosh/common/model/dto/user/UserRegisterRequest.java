package com.yosh.common.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 注册时仅收集登录必需的信息；昵称、头像等资料在登录后完善。
 */
@Data
public class UserRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录邮箱。
     */
    private String email;

    /**
     * 登录密码。
     */
    private String userPassword;

    /**
     * 确认密码。
     */
    private String checkPassword;

    /**
     * 邮箱验证码。
     */
    private String emailCode;

    /**
     * 可选的邀请码，由邀请链接解析后传入。
     */
    private String inviteCode;
}
