package com.yosh.server.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.yosh.common.exception.ErrorCode;
import com.yosh.common.exception.ThrowUtils;
import com.yosh.common.model.dto.file.UploadPictureResult;
import com.yosh.common.model.dto.picture.PictureQueryRequest;
import com.yosh.common.model.dto.picture.PictureUploadRequest;
import com.yosh.common.model.entry.Picture;
import com.yosh.common.model.entry.User;
import com.yosh.common.model.vo.LoginUserVO;
import com.yosh.common.model.vo.PictureVO;
import com.yosh.common.model.vo.UserVO;
import com.yosh.server.manger.FileManger;
import com.yosh.server.mapper.PictureMapper;
import com.yosh.server.service.PictureService;
import com.yosh.server.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author china_yosh
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2026-08-06 22:51:10
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService {
    @Resource
    private FileManger fileManger;
    @Resource
    private UserService userService;

    @Override
    public PictureVO uploadPicture(MultipartFile file, PictureUploadRequest uploadRequest, LoginUserVO loginUser){
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "上传文件为空");
        ThrowUtils.throwIf(uploadRequest == null, ErrorCode.PARAMS_ERROR, "上传参数为空");

        Long picId = uploadRequest.getId();
        Picture oldPicture = picId == null ? null : this.getById(picId);
        ThrowUtils.throwIf(picId != null && oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        final String prefix = String.format("public/%s", loginUser.getId()) ;
        UploadPictureResult uploadPictureResult = fileManger.uploadFile(file, prefix);

        Picture pic = Picture.builder()
                .id(picId)
                .url(uploadPictureResult.getUrl())
                .name(uploadPictureResult.getPicName())
                .picSize(uploadPictureResult.getPicSize())
                .picWidth(uploadPictureResult.getPicWidth())
                .picHeight(uploadPictureResult.getPicHeight())
                .picScale(uploadPictureResult.getPicScale())
                .picFormat(uploadPictureResult.getPicFormat())
                .userId(oldPicture == null ? loginUser.getId() : oldPicture.getUserId())
                .updateTime(new Date())
                .build();
        boolean result;
        if(picId == null){
            pic.setCreateTime(new Date());
            result = this.save(pic);
        }else {
            pic.setEditTime(new Date());
            result = this.updateById(pic);
        }
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片保存失败");

        return PictureVO.objToVo(pic);

    }
    @Override
    public LambdaQueryWrapper<Picture> getQueryWrapper(PictureQueryRequest request) {
        LambdaQueryWrapper<Picture> wrapper = new LambdaQueryWrapper<>();
        if (request == null) {
            return wrapper;
        }

        wrapper.and(StrUtil.isNotBlank(request.getSearchText()),
                        query -> query.like(Picture::getName, request.getSearchText())
                                .or()
                                .like(Picture::getIntroduction, request.getSearchText()))
                .eq(ObjUtil.isNotEmpty(request.getId()), Picture::getId, request.getId())
                .eq(ObjUtil.isNotEmpty(request.getUserId()), Picture::getUserId, request.getUserId())
                .like(StrUtil.isNotBlank(request.getName()), Picture::getName, request.getName())
                .like(StrUtil.isNotBlank(request.getIntroduction()),
                        Picture::getIntroduction, request.getIntroduction())
                .like(StrUtil.isNotBlank(request.getPicFormat()),
                        Picture::getPicFormat, request.getPicFormat())
                .eq(StrUtil.isNotBlank(request.getCategory()), Picture::getCategory, request.getCategory())
                .eq(ObjUtil.isNotEmpty(request.getPicWidth()), Picture::getPicWidth, request.getPicWidth())
                .eq(ObjUtil.isNotEmpty(request.getPicHeight()), Picture::getPicHeight, request.getPicHeight())
                .eq(ObjUtil.isNotEmpty(request.getPicSize()), Picture::getPicSize, request.getPicSize())
                .eq(ObjUtil.isNotEmpty(request.getPicScale()), Picture::getPicScale, request.getPicScale());

        if (CollUtil.isNotEmpty(request.getTags())) {
            request.getTags().forEach(tag -> wrapper.like(Picture::getTags, "\"" + tag + "\""));
        }

        boolean ascending = "ascend".equals(request.getSortOrder());
        String sortField = request.getSortField();
        if (StrUtil.isBlank(sortField)) {
            return wrapper.orderByDesc(Picture::getCreateTime);
        }
        return switch (sortField) {
            case "id" -> wrapper.orderBy(true, ascending, Picture::getId);
            case "name" -> wrapper.orderBy(true, ascending, Picture::getName);
            case "picSize" -> wrapper.orderBy(true, ascending, Picture::getPicSize);
            case "createTime" -> wrapper.orderBy(true, ascending, Picture::getCreateTime);
            case "updateTime" -> wrapper.orderBy(true, ascending, Picture::getUpdateTime);
            default -> wrapper.orderByDesc(Picture::getCreateTime);
        };
    }
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }
    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).getFirst();
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }





}




