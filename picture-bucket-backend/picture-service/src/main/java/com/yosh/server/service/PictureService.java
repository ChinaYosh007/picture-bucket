package com.yosh.server.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.yosh.common.model.dto.picture.PictureQueryRequest;
import com.yosh.common.model.dto.picture.PictureEditRequest;
import com.yosh.common.model.dto.picture.PictureReviewRequest;
import com.yosh.common.model.dto.picture.PictureUploadByBatchRequest;
import com.yosh.common.model.dto.picture.PictureUploadRequest;
import com.yosh.common.model.entry.Picture;
import com.yosh.common.model.entry.User;
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

    PictureVO uploadPicture(Object inputSources, PictureUploadRequest uploadRequest, LoginUserVO loginUser);

    LambdaQueryWrapper<Picture> getQueryWrapper(PictureQueryRequest request);

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, LoginUserVO loginUser);

    void fillReviewParms(Picture picture, LoginUserVO loginUser);

    void checkPictureAuth(LoginUserVO loginUser, Picture picture);

    void deletePicture(long pictureId, LoginUserVO loginUser);

    void editPicture(PictureEditRequest pictureEditRequest, LoginUserVO loginUser);

    void checkPictureQueryAuth(PictureQueryRequest pictureQueryRequest, LoginUserVO loginUser);
    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            LoginUserVO loginUser
    );
    /**
     * clear picture
     *
     */
    void clearPicture(Picture picture);

}
