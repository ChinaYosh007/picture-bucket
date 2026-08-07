package com.yosh.server.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.lang.Validator;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yosh.common.constants.UserConstant;
import com.yosh.common.enums.UserRoleEnum;
import com.yosh.common.exception.ErrorCode;
import com.yosh.common.exception.ThrowUtils;
import com.yosh.common.model.dto.user.UserAddRequest;
import com.yosh.common.model.dto.user.UserEditRequest;
import com.yosh.common.model.dto.user.UserLoginRequest;
import com.yosh.common.model.dto.user.UserPasswordUpdateRequest;
import com.yosh.common.model.dto.user.UserQueryRequest;
import com.yosh.common.model.dto.user.UserRegisterRequest;
import com.yosh.common.model.dto.user.UserUpdateRequest;
import com.yosh.common.model.entry.User;
import com.yosh.common.model.vo.LoginUserVO;
import com.yosh.common.model.vo.UserVO;
import com.yosh.common.request.DeleteRequest;
import com.yosh.common.responese.BaseResponse;
import com.yosh.common.responese.ResultUtils;
import com.yosh.server.annotation.AuthCheck;
import com.yosh.server.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/**
 * 用户认证与管理员用户管理接口。
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /** 使用邮箱验证码注册账号。 */
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest request) {
        return ResultUtils.success(userService.register(request));
    }

    /** 密码和邮箱验证码均校验通过后，创建登录会话。 */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginRequest request,
                                           HttpServletRequest httpRequest) {
        return ResultUtils.success(userService.userLogin(request, httpRequest));
    }

    /** 从当前 Cookie 对应的 Session 中读取登录用户。 */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        return ResultUtils.success(userService.getLoginUser(request));
    }

    /** 销毁当前会话，退出登录。 */
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        return ResultUtils.success(userService.userLogout(request));
    }

    /**
     * 发送邮箱验证码；保留 email 参数以兼容现有前端。
     */
    @PostMapping("/email-code")
    public BaseResponse<Boolean> sendEmailCode(
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "email", required = false) String email) {
        String target = StrUtil.isNotBlank(account) ? account : email;
        userService.sendEmailCode(target);
        return ResultUtils.success(true);
    }

    /** 管理员创建用户，密码仅在写入前加密。 */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.hasBlank(userAddRequest.getUserAccount(), userAddRequest.getUserPassword(),
                        userAddRequest.getEmail()),
                ErrorCode.PARAMS_ERROR, "账号、密码和邮箱不能为空");
        ThrowUtils.throwIf(StrUtil.length(userAddRequest.getUserPassword()) < 8
                        || StrUtil.length(userAddRequest.getUserPassword()) > 64,
                ErrorCode.PARAMS_ERROR, "密码长度不能少于 8 位");
        ThrowUtils.throwIf(!Validator.isEmail(userAddRequest.getEmail()),
                ErrorCode.PARAMS_ERROR, "邮箱格式不正确");

        String userAccount = normalizeUserAccount(userAddRequest.getUserAccount());
        String email = StrUtil.trim(userAddRequest.getEmail()).toLowerCase(Locale.ROOT);
        ThrowUtils.throwIf(userService.lambdaQuery().eq(User::getUserAccount, userAccount).exists(),
                ErrorCode.PARAMS_ERROR, "账号已存在");
        ThrowUtils.throwIf(userService.lambdaQuery().eq(User::getEmail, email).exists(),
                ErrorCode.PARAMS_ERROR, "邮箱已存在");

        String userRole = StrUtil.blankToDefault(userAddRequest.getUserRole(), UserConstant.DEFAULT_ROLE);
        ThrowUtils.throwIf(UserRoleEnum.getEnum(userRole) == null,
                ErrorCode.PARAMS_ERROR, "用户角色不合法");


        User user = User.builder().build();

        BeanUtils.copyProperties(userAddRequest, user);
        user.setUserPassword(userService.getEncryptPassword(userAddRequest.getUserPassword()));
        user.setUserAccount(userAccount);
        user.setEmail(email);
        user.setUserRole(userRole);
        String rawPassword = userAddRequest.getUserPassword();
        user.setUserPassword(userService.getEncryptPassword(rawPassword));
        ThrowUtils.throwIf(!userService.save(user), ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /** 管理端查询用户；保留原接口结构，但在响应前清空密码哈希。 */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(@RequestParam long id) {
        User user = getUser(id);
        user.setUserPassword(null);
        return ResultUtils.success(user);
    }

    /** 对外返回用户视图对象，不暴露实体中的敏感字段。 */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(@RequestParam long id) {
        return ResultUtils.success(getUserVO(id));
    }

    /** 普通用户编辑自己的基础资料（昵称/头像/简介），不允许改角色等敏感字段。 */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editUser(@RequestBody UserEditRequest userEditRequest,
                                          HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(userEditRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getLoginUser(httpRequest);

        User user = User.builder().build();
        user.setId(loginUser.getId());
        boolean identityChanged = false;

        if (userEditRequest.getUserAccount() != null) {
            String userAccount = normalizeUserAccount(userEditRequest.getUserAccount());
            ThrowUtils.throwIf(userService.lambdaQuery()
                            .eq(User::getUserAccount, userAccount)
                            .ne(User::getId, loginUser.getId())
                            .exists(),
                    ErrorCode.PARAMS_ERROR, "账号已存在");
            user.setUserAccount(userAccount);
            identityChanged = !userAccount.equals(loginUser.getUserAccount());
        }

        if (userEditRequest.getEmail() != null) {
            String email = StrUtil.trim(userEditRequest.getEmail()).toLowerCase(Locale.ROOT);
            ThrowUtils.throwIf(!Validator.isEmail(email), ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
            if (!email.equals(loginUser.getEmail())) {
                ThrowUtils.throwIf(userService.lambdaQuery()
                                .eq(User::getEmail, email)
                                .ne(User::getId, loginUser.getId())
                                .exists(),
                        ErrorCode.PARAMS_ERROR, "邮箱已存在");
                userService.verifyEmailCode(email, userEditRequest.getEmailCode());
                identityChanged = true;
            }
            user.setEmail(email);
        }

        user.setUserName(userEditRequest.getUserName());
        user.setUserAvatar(userEditRequest.getUserAvatar());
        user.setUserProfile(userEditRequest.getUserProfile());
        ThrowUtils.throwIf(!userService.updateById(user), ErrorCode.OPERATION_ERROR);
        if (identityChanged) {
            httpRequest.changeSessionId();
        }
        return ResultUtils.success(true);
    }

    /** 逻辑删除指定用户，仅管理员可操作。 */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null
                        || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        getUser(deleteRequest.getId());
        ThrowUtils.throwIf(!userService.removeById(deleteRequest.getId()), ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /** 更新用户的可编辑资料；请求对象不含密码，避免意外覆盖。 */
    /**
     * Update the current user's password and rotate the current session identifier.
     */
    @PostMapping("/password/update")
    public BaseResponse<Boolean> updatePassword(@RequestBody UserPasswordUpdateRequest passwordUpdateRequest,
                                                HttpServletRequest httpRequest) {
        LoginUserVO loginUser = userService.getLoginUser(httpRequest);
        ThrowUtils.throwIf(!userService.updateUserPassword(loginUser.getId(), passwordUpdateRequest),
                ErrorCode.OPERATION_ERROR);
        httpRequest.changeSessionId();
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null || userUpdateRequest.getId() == null
                        || userUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isNotBlank(userUpdateRequest.getEmail())
                        && !Validator.isEmail(userUpdateRequest.getEmail()),
                ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        ThrowUtils.throwIf(StrUtil.isNotBlank(userUpdateRequest.getUserRole())
                        && UserRoleEnum.getEnum(userUpdateRequest.getUserRole()) == null,
                ErrorCode.PARAMS_ERROR, "用户角色不合法");
        getUser(userUpdateRequest.getId());

        String userAccount = null;
        if (userUpdateRequest.getUserAccount() != null) {
            userAccount = normalizeUserAccount(userUpdateRequest.getUserAccount());
            ThrowUtils.throwIf(userService.lambdaQuery()
                            .eq(User::getUserAccount, userAccount)
                            .ne(User::getId, userUpdateRequest.getId())
                            .exists(),
                    ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        String email = null;
        if (userUpdateRequest.getEmail() != null) {
            email = StrUtil.trim(userUpdateRequest.getEmail()).toLowerCase(Locale.ROOT);
            ThrowUtils.throwIf(userService.lambdaQuery()
                            .eq(User::getEmail, email)
                            .ne(User::getId, userUpdateRequest.getId())
                            .exists(),
                    ErrorCode.PARAMS_ERROR, "邮箱已存在");
        }

        User user = User.builder().build();
        BeanUtils.copyProperties(userUpdateRequest, user);
        user.setUserAccount(userAccount);
        user.setEmail(email);
        ThrowUtils.throwIf(!userService.updateById(user), ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /** 管理端分页查询，排序字段由服务层白名单控制。 */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        ThrowUtils.throwIf(current < 1 || pageSize < 1 || pageSize > 50, ErrorCode.PARAMS_ERROR);

        Page<User> userPage = userService.page(
                new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /** 统一按 id 查询并处理不存在的情况，供视图查询复用。 */
    private UserVO getUserVO(long id) {
        return userService.getUserVO(getUser(id));
    }

    private User getUser(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    /**
     * 账号既用于登录，也用于通过账号发送验证码；统一限制格式避免空白或特殊字符账号。
     */
    private String normalizeUserAccount(String userAccount) {
        String normalizedAccount = StrUtil.trim(userAccount);
        ThrowUtils.throwIf(StrUtil.isBlank(normalizedAccount)
                        || normalizedAccount.length() < 3
                        || normalizedAccount.length() > 64
                        || !normalizedAccount.matches("[A-Za-z0-9_-]+"),
                ErrorCode.PARAMS_ERROR,
                "账号仅支持 3 至 64 位字母、数字、下划线或连字符");
        return normalizedAccount;
    }
}
