package com.yosh.common.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request for a logged-in user to update their password.
 */
@Data
public class UserPasswordUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String currentPassword;

    private String newPassword;

    private String confirmPassword;
}
