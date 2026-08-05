package com.yosh.server.aspect;

import com.yosh.common.enums.UserRoleEnum;
import com.yosh.common.exception.ErrorCode;
import com.yosh.common.exception.ThrowUtils;
import com.yosh.common.model.vo.LoginUserVO;
import com.yosh.server.annotation.AuthCheck;
import com.yosh.server.service.UserService;
import jakarta.annotation.Resource;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object before(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 未登录时 getLoginUser 会直接抛异常
        LoginUserVO loginUser = userService.getLoginUser(request);

        // 配置了角色才校验；未配置角色时只要求登录
        if (mustRole != null && !mustRole.isBlank()) {
            UserRoleEnum requiredRole = UserRoleEnum.getEnum(mustRole);

            // 防止注解里写了不存在的角色，结果意外放行
            ThrowUtils.throwIf(requiredRole == null,
                    ErrorCode.SYSTEM_ERROR,
                    "权限角色配置错误");

            ThrowUtils.throwIf(
                    !requiredRole.getValue().equals(loginUser.getUserRole()),
                    ErrorCode.NO_AUTH_ERROR
            );
        }

        return joinPoint.proceed();
    }
}
