package com.yosh.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yosh.common.constants.UserConstant;
import com.yosh.common.exception.BusinessException;
import com.yosh.common.exception.ErrorCode;
import com.yosh.common.exception.ThrowUtils;
import com.yosh.common.model.dto.space.SpaceAddRequest;
import com.yosh.common.model.dto.space.SpaceEditRequest;
import com.yosh.common.model.dto.space.SpaceQueryRequest;
import com.yosh.common.model.dto.space.SpaceUpdateRequest;
import com.yosh.common.model.entry.Space;
import com.yosh.common.model.vo.LoginUserVO;
import com.yosh.common.model.vo.SpaceVO;
import com.yosh.common.model.vo.SpaceLevel;
import com.yosh.common.enums.SpaceLevelEnum;
import com.yosh.common.request.DeleteRequest;
import com.yosh.common.responese.BaseResponse;
import com.yosh.common.responese.ResultUtils;
import com.yosh.server.annotation.AuthCheck;
import com.yosh.server.service.SpaceService;
import com.yosh.server.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/space")
public class SpaceController {
    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest,
                                       HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getLoginUser(request);
        long spaceId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(spaceId);
    }

    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .toList();
        return ResultUtils.success(spaceLevelList);
    }
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest,
                                              HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null
                || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Space oldSpace = getSpace(deleteRequest.getId());
        LoginUserVO loginUser = userService.getLoginUser(request);
        checkSpaceAuth(oldSpace, loginUser);
        ThrowUtils.throwIf(!spaceService.removeById(oldSpace.getId()), ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() == null
                || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        // 自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 数据校验
        spaceService.validSpace(space, false);
        // 判断是否存在
        long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(@RequestParam long id) {
        return ResultUtils.success(getSpace(id));
    }

    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(@RequestParam long id,
                                                HttpServletRequest request) {
        Space space = getSpace(id);
        return ResultUtils.success(spaceService.getSpaceVO(space, request));
    }

    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        checkPageRequest(spaceQueryRequest);
        Page<Space> spacePage = spaceService.page(
                new Page<>(spaceQueryRequest.getCurrent(), spaceQueryRequest.getPageSize()),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                         HttpServletRequest request) {
        checkPageRequest(spaceQueryRequest);
        Page<Space> spacePage = spaceService.page(
                new Page<>(spaceQueryRequest.getCurrent(), spaceQueryRequest.getPageSize()),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage, request));
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest,
                                           HttpServletRequest request) {
        ThrowUtils.throwIf(spaceEditRequest == null || spaceEditRequest.getId() == null
                || spaceEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Space oldSpace = getSpace(spaceEditRequest.getId());
        LoginUserVO loginUser = userService.getLoginUser(request);
        checkSpaceAuth(oldSpace, loginUser);

        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);
        spaceService.validSpace(space, false);
        ThrowUtils.throwIf(!spaceService.updateById(space), ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    private Space getSpace(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        return space;
    }

    private void checkSpaceAuth(Space space, LoginUserVO loginUser) {
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    private void checkPageRequest(SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = spaceQueryRequest.getCurrent();
        long pageSize = spaceQueryRequest.getPageSize();
        ThrowUtils.throwIf(current < 1 || pageSize < 1 || pageSize > 50, ErrorCode.PARAMS_ERROR);
    }

}
