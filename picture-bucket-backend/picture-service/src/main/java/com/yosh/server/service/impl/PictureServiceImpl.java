package com.yosh.server.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.yosh.common.enums.PictureReviewStatusEnum;
import com.yosh.common.exception.BusinessException;
import com.yosh.common.exception.ErrorCode;
import com.yosh.common.exception.ThrowUtils;
import com.yosh.common.model.dto.file.UploadPictureResult;
import com.yosh.common.model.dto.picture.PictureQueryRequest;
import com.yosh.common.model.dto.picture.PictureReviewRequest;
import com.yosh.common.model.dto.picture.PictureUploadByBatchRequest;
import com.yosh.common.model.dto.picture.PictureUploadRequest;
import com.yosh.common.model.entry.Picture;
import com.yosh.common.model.entry.User;
import com.yosh.common.model.vo.LoginUserVO;
import com.yosh.common.model.vo.PictureVO;
import com.yosh.common.model.vo.UserVO;

import com.yosh.server.manger.upload.FilePictureUpload;
import com.yosh.server.manger.upload.UrlPictureUpload;
import com.yosh.server.mapper.PictureMapper;
import com.yosh.server.service.PictureService;
import com.yosh.server.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private UserService userService;

    @Override
    public PictureVO uploadPicture(Object inputSources, PictureUploadRequest uploadRequest, LoginUserVO loginUser){
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(inputSources == null, ErrorCode.PARAMS_ERROR, "上传文件为空");
        ThrowUtils.throwIf(uploadRequest == null, ErrorCode.PARAMS_ERROR, "上传参数为空");

        Long picId = uploadRequest.getId();
        Picture oldPicture = picId == null ? null : this.getById(picId);
        ThrowUtils.throwIf(picId != null && oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        if (oldPicture != null) {
            ThrowUtils.throwIf(!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        }
        final String prefix = String.format("public/%s", loginUser.getId());

        UploadPictureResult uploadPictureResult = null;
        if (inputSources instanceof MultipartFile) {
            uploadPictureResult = filePictureUpload.uploadFile(inputSources, prefix);
        } else if (inputSources instanceof String) {
            uploadPictureResult = urlPictureUpload.uploadFile(inputSources, prefix);
        } else {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "不支持的上传类型");
        }
        Picture pic = Picture.builder()
                .id(picId)
                .url(uploadPictureResult.getUrl())
                .name(StrUtil.isNotBlank(uploadRequest.getName()) ? uploadRequest.getName() : uploadPictureResult.getPicName() )
                .picSize(uploadPictureResult.getPicSize())
                .picWidth(uploadPictureResult.getPicWidth())
                .picHeight(uploadPictureResult.getPicHeight())
                .picScale(uploadPictureResult.getPicScale())
                .picFormat(uploadPictureResult.getPicFormat())
                .userId(oldPicture == null ? loginUser.getId() : oldPicture.getUserId())
                .updateTime(new Date())
                .build();
        this.fillReviewParms(pic, loginUser);

        boolean result;
        if (picId == null) {
            pic.setCreateTime(new Date());
            result = this.save(pic);
        } else {
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
                .like(StrUtil.isNotBlank(request.getReviewMessage()),Picture::getReviewMessage, request.getReviewMessage())
                .eq(StrUtil.isNotBlank(request.getCategory()), Picture::getCategory, request.getCategory())
                .eq(ObjUtil.isNotEmpty(request.getPicWidth()), Picture::getPicWidth, request.getPicWidth())
                .eq(ObjUtil.isNotEmpty(request.getPicHeight()), Picture::getPicHeight, request.getPicHeight())
                .eq(ObjUtil.isNotEmpty(request.getPicSize()), Picture::getPicSize, request.getPicSize())
                .eq(ObjUtil.isNotEmpty(request.getPicSize()), Picture::getReviewStatus, request.getReviewStatus())
                .eq(ObjUtil.isNotEmpty(request.getPicSize()), Picture::getReviewerId, request.getReviewerId())
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

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, LoginUserVO loginUser) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.OPERATION_ERROR, "参数为空");
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();

        PictureReviewStatusEnum enumByValue = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        ThrowUtils.throwIf(id == null || enumByValue == null || PictureReviewStatusEnum.REVIEWING.equals(enumByValue)
                        , ErrorCode.OPERATION_ERROR , "参数为空");
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        if(!oldPicture.getReviewStatus().equals(reviewStatus)){
            throw  new BusinessException(ErrorCode.OPERATION_ERROR, "图片状态已改变");
        }
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, picture);
        picture.setReviewerId(loginUser.getId());
        picture.setReviewTime(new Date());
        Boolean res = this.updateById(picture);
        ThrowUtils.throwIf(!BooleanUtil.isTrue( res), ErrorCode.OPERATION_ERROR);

    }
    @Override
    public void fillReviewParms(Picture picture, LoginUserVO loginUser){
        if(userService.isAdmin(loginUser)){
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员审核通过");
        }else{
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, LoginUserVO loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if(StrUtil.isBlank(namePrefix)){
            namePrefix = searchText;
        }
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多上传30张图片");

        // 对搜索词做 URL 编码
        String encodedSearch = URLEncoder.encode(searchText, StandardCharsets.UTF_8);
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", encodedSearch);
        try {
            // 抓取内容
            Document document = Jsoup.connect(fetchUrl).get();
            // 解析内容
            Element dgControl = document.getElementsByClass("dgControl").first();
            if(ObjectUtil.isEmpty(dgControl)){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未获取到图片内容");
            }
            AtomicInteger size = new AtomicInteger();
            String finalNamePrefix = namePrefix;
            // 用 select 做 CSS 选择器匹配，而非 getElementsByClass
            assert dgControl != null;
            dgControl.select("img.mimg")
                     .stream()
                     .map(element -> element.attr("src"))
                     .filter(src -> StrUtil.isNotBlank(src) && !src.startsWith("data:"))
                     .map(url -> {
                         int index = url.indexOf("?");
                         String uri = index > 0 ? url.substring(0, index) : url;
                         return PictureUploadRequest.builder()
                                 .url(uri)
                                 .name(finalNamePrefix + "_" + (size.getAndIncrement() + 1) + ".jpg")
                                 .build();
                     })
                     .limit(count)
                     .forEach(pic -> {
                         try {
                             uploadPicture(pic.getUrl(), pic, loginUser);
                         } catch (Exception e) {
                             log.error("上传图片失败", e);
                         }
                     });
            return size.get();

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "抓取 Bing 图片失败");
        }
    }


}




