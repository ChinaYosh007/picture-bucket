package com.yosh.common.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户编辑自己的资料请求（仅允许修改基础展示字段，不含角色、邮箱等敏感字段）。
 */
@Data
public class UserEditRequest implements Serializable {

    /**
     * 登录账号
     */
    private String userAccount;

    /**
     * 绑定邮箱
     */
    private String email;

    /**
     * 新邮箱验证码，仅在变更绑定邮箱时需要
     */
    private String emailCode;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    private static final long serialVersionUID = 1L;
}
