package com.yosh.server.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.yosh.common.enums.SpaceLevelEnum;
import com.yosh.common.exception.BusinessException;
import com.yosh.common.exception.ErrorCode;
import com.yosh.common.exception.ThrowUtils;
import com.yosh.common.model.dto.space.SpaceAddRequest;
import com.yosh.common.model.dto.space.SpaceQueryRequest;
import com.yosh.common.model.entry.Space;
import com.yosh.common.model.entry.User;
import com.yosh.common.model.vo.LoginUserVO;
import com.yosh.common.model.vo.SpaceVO;
import com.yosh.server.mapper.SpaceMapper;
import com.yosh.server.service.SpaceService;
import com.yosh.server.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author china_yosh
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2026-08-12 15:46:11
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService {

    @Resource
    private UserService userService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, LoginUserVO loginUserVO) {
        Space space = BeanUtil.copyProperties(spaceAddRequest, Space.class);
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (null == space.getSpaceLevel()) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        fillSpaceBySpaceLevel(space);
        validSpace(space, true);
        Long userId = loginUserVO.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUserVO)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员用户不能创建私密空间");
        }
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                boolean exists = this.lambdaQuery().eq(Space::getUserId, userId)
                        .exists();
                if (exists) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已存在空间");
                }
                save(space);
                return space.getId();

            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 要创建
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
        }
        // 修改数据时，如果要改空间级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
    }
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public LambdaQueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper.orderByDesc(Space::getCreateTime);
        }

        queryWrapper.eq(ObjUtil.isNotNull(spaceQueryRequest.getId()),
                        Space::getId, spaceQueryRequest.getId())
                .eq(ObjUtil.isNotNull(spaceQueryRequest.getUserId()),
                        Space::getUserId, spaceQueryRequest.getUserId())
                .like(StrUtil.isNotBlank(spaceQueryRequest.getSpaceName()),
                        Space::getSpaceName, spaceQueryRequest.getSpaceName())
                .eq(ObjUtil.isNotNull(spaceQueryRequest.getSpaceLevel()),
                        Space::getSpaceLevel, spaceQueryRequest.getSpaceLevel());

        boolean ascending = "ascend".equals(spaceQueryRequest.getSortOrder());
        String sortField = spaceQueryRequest.getSortField();
        if (StrUtil.isBlank(sortField)) {
            return queryWrapper.orderByDesc(Space::getCreateTime);
        }
        return switch (sortField) {
            case "id" -> queryWrapper.orderBy(true, ascending, Space::getId);
            case "spaceName" -> queryWrapper.orderBy(true, ascending, Space::getSpaceName);
            case "spaceLevel" -> queryWrapper.orderBy(true, ascending, Space::getSpaceLevel);
            case "totalSize" -> queryWrapper.orderBy(true, ascending, Space::getTotalSize);
            case "totalCount" -> queryWrapper.orderBy(true, ascending, Space::getTotalCount);
            case "createTime" -> queryWrapper.orderBy(true, ascending, Space::getCreateTime);
            case "updateTime" -> queryWrapper.orderBy(true, ascending, Space::getUpdateTime);
            default -> queryWrapper.orderByDesc(Space::getCreateTime);
        };
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        if (spaceVO == null) {
            return null;
        }
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            spaceVO.setUser(userService.getUserVO(userService.getById(userId)));
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        List<Space> spaceList = spacePage.getRecords();
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }

        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        Set<Long> userIdSet = spaceList.stream()
                .map(Space::getUserId)
                .filter(ObjUtil::isNotNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        spaceVOList.forEach(spaceVO ->
                spaceVO.setUser(userService.getUserVO(userMap.get(spaceVO.getUserId()))));
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public Space getSpaceAndCheckAuth(Long spaceId, LoginUserVO loginUser) {
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Space space = this.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "没有空间权限");
        return space;
    }

    @Override
    public void checkSpaceQuota(Space space, long sizeChange, long countChange) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        long totalSize = ObjUtil.defaultIfNull(space.getTotalSize(), 0L);
        long totalCount = ObjUtil.defaultIfNull(space.getTotalCount(), 0L);
        ThrowUtils.throwIf(totalCount + countChange > space.getMaxCount(),
                ErrorCode.OPERATION_ERROR, "空间条数不足");
        ThrowUtils.throwIf(totalSize + sizeChange > space.getMaxSize(),
                ErrorCode.OPERATION_ERROR, "空间大小不足");
    }

    @Override
    public void updateSpaceUsage(Long spaceId, long sizeChange, long countChange) {
        if (spaceId == null) {
            return;
        }
        var updateWrapper = this.lambdaUpdate()
                .eq(Space::getId, spaceId)
                .setSql("totalSize = GREATEST(totalSize + " + sizeChange + ", 0)")
                .setSql("totalCount = GREATEST(totalCount + " + countChange + ", 0)");
        ThrowUtils.throwIf(!updateWrapper.update(), ErrorCode.OPERATION_ERROR, "额度更新失败");
    }



}



