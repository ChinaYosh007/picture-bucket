package com.yosh.common.constants;

public interface UserConstant {
    String PROFILE = """
            该用户比较懒，什么都没有写
            """;
    String USER_PREFIX = "fka";
    String EMAIL_CODE_PREFIX = "email:code:";
    String EMAIL_CODE_SEND_LIMIT_PREFIX = "email:code:send-limit:";
    int EMAIL_CODE_EXPIRE_TIME = 5 * 60;
    /**
     * 验证码发送令牌桶容量。
     */
    int EMAIL_CODE_SEND_BUCKET_CAPACITY = 5;
    /**
     * 令牌桶从空到满的补充周期（秒）。
     */
    int EMAIL_CODE_SEND_BUCKET_REFILL_PERIOD_TIME = 60;

    /**
     * Session 中登录用户的 key
     */
    String USER_LOGIN_STATE = "user_login";

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

}
