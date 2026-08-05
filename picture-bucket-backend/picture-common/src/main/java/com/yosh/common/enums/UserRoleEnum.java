package com.yosh.common.enums;

import lombok.Getter;

import java.util.Map;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRoleEnum {
    USER("user", "user"),
    ADMIN("admin", "admin");

    private final String text;
    private final String value;
    private static final Map<String, UserRoleEnum> VALUE_ENUM_MAP = Map.of(
            USER.value, USER,
            ADMIN.value, ADMIN
    );

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static UserRoleEnum getEnum(String value) {
        return value == null ? null : VALUE_ENUM_MAP.get(value);
    }
}
