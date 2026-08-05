package com.yosh.server.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.yosh.common.constants.UserConstant;
import com.yosh.common.enums.UserRoleEnum;
import com.yosh.common.exception.BusinessException;
import com.yosh.common.exception.ErrorCode;
import com.yosh.common.exception.ThrowUtils;
import com.yosh.common.model.core.MailCore;
import com.yosh.common.model.dto.user.UserLoginRequest;
import com.yosh.common.model.dto.user.UserQueryRequest;
import com.yosh.common.model.dto.user.UserRegisterRequest;
import com.yosh.common.model.entry.User;
import com.yosh.common.model.vo.LoginUserVO;
import com.yosh.common.model.vo.UserVO;
import com.yosh.server.mapper.UserMapper;
import com.yosh.server.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /** BCrypt 自带随机盐，用于安全地保存用户密码。 */
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /** 验证码和登录相关短期数据的 Redis 访问入口。 */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 每个邮箱独立限流：容量 5 个令牌，60 秒补满。 */
    @Resource(name = "emailCodeTokenBucketScript")
    private DefaultRedisScript<Long> emailCodeTokenBucketScript;

    /** Spring 根据 spring.mail 配置创建的邮件发送器。 */
    @Resource
    private MailSender mailSender;

    /** 集中提供发件人地址，业务逻辑中不硬编码邮箱。 */
    @Resource
    private MailCore mailCore;

    /**
     * 根据允许的字段构建 Lambda 查询条件，避免将客户端排序字段直接拼接到 SQL 中。
     */
    @Override
    public LambdaQueryWrapper<User> getQueryWrapper(UserQueryRequest queryRequest) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (queryRequest == null) {
            return queryWrapper.orderByDesc(User::getCreateTime);
        }

        queryWrapper.eq(ObjUtil.isNotNull(queryRequest.getId()), User::getId, queryRequest.getId());
        queryWrapper.like(StrUtil.isNotBlank(queryRequest.getUserAccount()),
                User::getUserAccount, queryRequest.getUserAccount());
        queryWrapper.like(StrUtil.isNotBlank(queryRequest.getUserName()),
                User::getUserName, queryRequest.getUserName());
        queryWrapper.like(StrUtil.isNotBlank(queryRequest.getUserProfile()),
                User::getUserProfile, queryRequest.getUserProfile());
        queryWrapper.eq(StrUtil.isNotBlank(queryRequest.getUserRole()),
                User::getUserRole, queryRequest.getUserRole());

        boolean ascending = "ascend".equals(queryRequest.getSortOrder());
        String sortField = queryRequest.getSortField();
        if (StrUtil.isBlank(sortField)) {
            return queryWrapper.orderByDesc(User::getCreateTime);
        }

        return switch (sortField) {
            case "id" -> queryWrapper.orderBy(true, ascending, User::getId);
            case "userAccount" -> queryWrapper.orderBy(true, ascending, User::getUserAccount);
            case "userName" -> queryWrapper.orderBy(true, ascending, User::getUserName);
            case "userRole" -> queryWrapper.orderBy(true, ascending, User::getUserRole);
            case "createTime" -> queryWrapper.orderBy(true, ascending, User::getCreateTime);
            case "updateTime" -> queryWrapper.orderBy(true, ascending, User::getUpdateTime);
            default -> queryWrapper.orderByDesc(User::getCreateTime);
        };
    }
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }


    @Override
    public LoginUserVO userLogin(UserLoginRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "登录参数不能为空");

        String account = StrUtil.trim(request.getAccount());
        String userPassword = request.getUserPassword();
        String emailCode = StrUtil.trim(request.getEmailCode());

        ThrowUtils.throwIf(StrUtil.hasBlank(account, userPassword, emailCode),
                ErrorCode.PARAMS_ERROR,
                "账号/邮箱、密码和验证码不能为空");
        ThrowUtils.throwIf(userPassword.length() < 8 || userPassword.length() > 64,
                ErrorCode.PARAMS_ERROR,
                "密码错误");
        ThrowUtils.throwIf(emailCode.length() != 6,
                ErrorCode.PARAMS_ERROR,
                "验证码格式不正确");

        // 账号既支持邮箱，也支持系统生成的 userAccount。
        User user = findUserByAccountOrEmail(account);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");

        if (!PASSWORD_ENCODER.matches(userPassword, user.getUserPassword())) {
            log.info("user login failed, password mismatch, account={}", account);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 登录只校验用户主动获取的验证码，不能在这里再次发送验证码。
        // getAndDelete 保证验证码成功使用后立即失效，防止重放。
        String emailCodeKey = UserConstant.EMAIL_CODE_PREFIX + user.getEmail();
        String savedEmailCode = stringRedisTemplate.opsForValue().getAndDelete(emailCodeKey);
        ThrowUtils.throwIf(!Objects.equals(emailCode, savedEmailCode),
                ErrorCode.PARAMS_ERROR,
                "邮箱验证码错误或已失效");

        // 更换 Session ID，降低会话固定攻击风险。
        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        // Redis Session 中只保存用户 ID，避免存入密码哈希等敏感字段。
        session.setAttribute(UserConstant.USER_LOGIN_STATE, user.getId());
        return getLoginUserVO(user);
    }

    @Override
    public LoginUserVO getLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        ThrowUtils.throwIf(session == null, ErrorCode.NOT_LOGIN_ERROR);

        Object userIdObj = session.getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(!(userIdObj instanceof Number), ErrorCode.NOT_LOGIN_ERROR);
        // Session 只保存 id，每次读取数据库最新用户信息。
        User user = this.getById(((Number) userIdObj).longValue());
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        return getLoginUserVO(user);
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object userObj = session == null ? null : session.getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(userObj == null, ErrorCode.OPERATION_ERROR, "未登录");
        session.invalidate();
        return true;
    }

    @Override
    @Transactional
    public long register(UserRegisterRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "注册参数不能为空");

        String userPassword = request.getUserPassword();
        String checkPassword = request.getCheckPassword();
        String email = normalizeEmail(request.getEmail());
        String emailCode = StrUtil.trim(request.getEmailCode());

        ThrowUtils.throwIf(StrUtil.isBlank(userPassword)
                        || StrUtil.isBlank(checkPassword)
                        || StrUtil.isBlank(emailCode),
                ErrorCode.PARAMS_ERROR,
                "密码和邮箱验证码不能为空");
        ThrowUtils.throwIf(userPassword.length() < 8 || userPassword.length() > 64,
                ErrorCode.PARAMS_ERROR,
                "密码长度必须为 8 到 64 位");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword),
                ErrorCode.PARAMS_ERROR,
                "两次输入的密码不一致");

        // 数据库唯一约束是最终兜底，这里提前给出可读的业务提示。
        Long existingUserCount = baseMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        ThrowUtils.throwIf(existingUserCount != null && existingUserCount > 0,
                ErrorCode.PARAMS_ERROR,
                "邮箱已注册");

        Long inviteUserId = getInviteUserId(request.getInviteCode());

        // 注册验证码也只能成功使用一次。
        String emailCodeKey = UserConstant.EMAIL_CODE_PREFIX + email;
        String savedEmailCode = stringRedisTemplate.opsForValue().getAndDelete(emailCodeKey);
        ThrowUtils.throwIf(!Objects.equals(emailCode, savedEmailCode),
                ErrorCode.PARAMS_ERROR,
                "邮箱验证码错误或已失效");

        User newUser = User.builder()
                .userAccount(generateUserAccount())
                .userPassword(getEncryptPassword(userPassword))
                .email(email)
                .userName(UserConstant.USER_PREFIX + RandomUtil.randomString(10))
                .userProfile(UserConstant.PROFILE)
                .userRole(UserRoleEnum.USER.getValue())
                .shareCode(generateShareCode())
                .inviteUser(inviteUserId)
                .build();
        try {
            ThrowUtils.throwIf(!this.save(newUser), ErrorCode.SYSTEM_ERROR, "注册失败");
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱或内部标识冲突，请重试");
        }
        return newUser.getId();
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        return PASSWORD_ENCODER.encode(userPassword);
    }

    @Override
    public void sendEmailCode(String accountOrEmail) {
        String email = resolveEmailForCode(accountOrEmail);

        String sendLimitKey = UserConstant.EMAIL_CODE_SEND_LIMIT_PREFIX + email;
        Long tokenConsumed = stringRedisTemplate.execute(
                emailCodeTokenBucketScript,
                Collections.singletonList(sendLimitKey),
                String.valueOf(UserConstant.EMAIL_CODE_SEND_BUCKET_CAPACITY),
                String.valueOf(Duration.ofSeconds(UserConstant.EMAIL_CODE_SEND_BUCKET_REFILL_PERIOD_TIME).toMillis()),
                String.valueOf(System.currentTimeMillis()));
        ThrowUtils.throwIf(!Long.valueOf(1).equals(tokenConsumed),
                ErrorCode.OPERATION_ERROR,
                "验证码请求过于频繁，请稍后再试");

        String emailCode = RandomUtil.randomNumbers(6);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailCore.getMailFrom());
        message.setTo(email);
        message.setSubject("Picture Bucket 验证码");
        message.setText("你的验证码为：" + emailCode + "，5 分钟内有效，请勿泄露给他人。");

        try {
             mailSender.send(message);
        } catch (RuntimeException exception) {
            log.error("邮箱验证码发送失败", exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "验证码发送失败，请稍后重试");
        }
        stringRedisTemplate.opsForValue().set(
                UserConstant.EMAIL_CODE_PREFIX + email,
                emailCode,
                Duration.ofSeconds(UserConstant.EMAIL_CODE_EXPIRE_TIME));
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        return LoginUserVO.builder()
                .id(user.getId())
                .userAccount(user.getUserAccount())
                .email(user.getEmail())
                .userName(user.getUserName())
                .userAvatar(user.getUserAvatar())
                .userProfile(user.getUserProfile())
                .userRole(user.getUserRole())
                .vipExpireTime(user.getVipExpireTime())
                .vipCode(user.getVipCode())
                .vipNumber(user.getVipNumber())
                .shareCode(user.getShareCode())
                .createTime(user.getCreateTime())
                .build();
    }

    /**
     * 解析收件邮箱：入参是邮箱则规范化；否则按账号查用户邮箱。
     */
    private String resolveEmailForCode(String accountOrEmail) {
        String target = StrUtil.trim(accountOrEmail);
        ThrowUtils.throwIf(StrUtil.isBlank(target), ErrorCode.PARAMS_ERROR, "请输入邮箱或账号");

        if (Validator.isEmail(target)) {
            return normalizeEmail(target);
        }

        User user = baseMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUserAccount, target));
        ThrowUtils.throwIf(user == null || StrUtil.isBlank(user.getEmail()),
                ErrorCode.PARAMS_ERROR,
                "账号不存在或未绑定邮箱");
        return normalizeEmail(user.getEmail());
    }

    private User findUserByAccountOrEmail(String accountOrEmail) {
        String target = StrUtil.trim(accountOrEmail);
        if (StrUtil.isBlank(target)) {
            return null;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (Validator.isEmail(target)) {
            wrapper.eq(User::getEmail, target.toLowerCase(Locale.ROOT));
        } else {
            wrapper.eq(User::getUserAccount, target);
        }
        return baseMapper.selectOne(wrapper);
    }

    private String normalizeEmail(String email) {
        String normalizedEmail = StrUtil.trim(email);
        ThrowUtils.throwIf(StrUtil.isBlank(normalizedEmail) || !Validator.isEmail(normalizedEmail),
                ErrorCode.PARAMS_ERROR,
                "邮箱格式不正确");
        return normalizedEmail.toLowerCase(Locale.ROOT);
    }

    private Long getInviteUserId(String inviteCode) {
        if (StrUtil.isBlank(inviteCode)) {
            return null;
        }
        User inviteUser = baseMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getShareCode, StrUtil.trim(inviteCode)));
        ThrowUtils.throwIf(inviteUser == null, ErrorCode.PARAMS_ERROR, "邀请码无效");
        return inviteUser.getId();
    }

    private String generateShareCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String shareCode = RandomUtil.randomString(10).toUpperCase(Locale.ROOT);
            Long existingCount = baseMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getShareCode, shareCode));
            if (existingCount == null || existingCount == 0) {
                return shareCode;
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成邀请码失败，请重试");
    }

    private String generateUserAccount() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String userAccount = UserConstant.USER_PREFIX + "_" + RandomUtil.randomString(12).toLowerCase(Locale.ROOT);
            Long existingCount = baseMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getUserAccount, userAccount));
            if (existingCount == null || existingCount == 0) {
                return userAccount;
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成用户账号失败，请重试");
    }
}
