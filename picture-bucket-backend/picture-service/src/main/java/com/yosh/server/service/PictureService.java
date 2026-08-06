package com.yosh.server.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.yosh.common.model.dto.picture.PictureQueryRequest;
import com.yosh.common.model.dto.picture.PictureUploadRequest;
import com.yosh.common.model.entry.Picture;
import com.yosh.common.model.vo.LoginUserVO;
import com.yosh.common.model.vo.PictureVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

/**
* @author china_yosh
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-08-06 22:51:10
*/
public interface PictureService extends IService<Picture> {

    PictureVO uploadPicture(MultipartFile file, PictureUploadRequest uploadRequest, LoginUserVO loginUser);

    LambdaQueryWrapper<Picture> getQueryWrapper(PictureQueryRequest request);

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    void validPicture(Picture picture);
}
