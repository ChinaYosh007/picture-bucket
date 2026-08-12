package com.yosh.server.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.yosh.common.model.dto.space.SpaceAddRequest;
import com.yosh.common.model.dto.space.SpaceQueryRequest;
import com.yosh.common.model.entry.Space;
import com.yosh.common.model.vo.LoginUserVO;
import com.yosh.common.model.vo.SpaceVO;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author china_yosh
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-08-12 15:46:11
*/
public interface SpaceService extends IService<Space> {

    long addSpace(SpaceAddRequest spaceAddRequest, LoginUserVO loginUserVO);

    void validSpace(Space space, boolean add);

    void fillSpaceBySpaceLevel(Space space);

    LambdaQueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    Space getSpaceAndCheckAuth(Long spaceId, LoginUserVO loginUser);

    void checkSpaceQuota(Space space, long sizeChange, long countChange);

    void updateSpaceUsage(Long spaceId, long sizeChange, long countChange);
}
