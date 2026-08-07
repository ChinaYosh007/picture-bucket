package com.yosh.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.IService;
import com.yosh.common.model.dto.user.UserLoginRequest;
import com.yosh.common.model.dto.user.UserPasswordUpdateRequest;
import com.yosh.common.model.dto.user.UserQueryRequest;
import com.yosh.common.model.dto.user.UserRegisterRequest;
import com.yosh.common.model.entry.User;
import com.yosh.common.model.vo.LoginUserVO;
import com.yosh.common.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/** 用户注册、登录与邮箱验证码服务。 */
public interface UserService extends IService<User> {

    /** 通过邮箱、密码和验证码注册用户。 */
    long register(UserRegisterRequest request);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 构建管理端用户分页查询条件。
     */
    LambdaQueryWrapper<User> getQueryWrapper(UserQueryRequest queryRequest);

    /**
     * 用户登录：邮箱或账号 + 密码 + 邮箱验证码。
     */
    LoginUserVO userLogin(UserLoginRequest request, HttpServletRequest httpRequest);

    /** 获取当前登录用户（脱敏）。 */
    LoginUserVO getLoginUser(HttpServletRequest request);

    /** 退出登录。 */
    boolean userLogout(HttpServletRequest request);

    /** 加密用户密码。 */
    String getEncryptPassword(String userPassword);

    boolean updateUserPassword(long userId, UserPasswordUpdateRequest request);

    /**
     * 发送邮箱验证码。
     * {@code accountOrEmail} 可为邮箱，或已注册用户的账号（按账号查邮箱后发送）。
     */
    void sendEmailCode(String accountOrEmail);

    /** Verify and consume an email verification code. */
    void verifyEmailCode(String email, String emailCode);

    /** 实体转登录视图。 */
    LoginUserVO getLoginUserVO(User user);
    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(LoginUserVO user);


}
