package com.yosh.common.model.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 登录用户脱敏视图，不包含密码。
 */
@Data
@Builder
public class LoginUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String userAccount;
    private String email;
    private String userName;
    private String userAvatar;
    private String userProfile;
    private String userRole;
    private Date vipExpireTime;
    private String vipCode;
    private Long vipNumber;
    private String shareCode;
    private Date createTime;
}
