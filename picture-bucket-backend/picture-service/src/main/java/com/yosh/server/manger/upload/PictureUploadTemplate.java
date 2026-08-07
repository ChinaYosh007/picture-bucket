package com.yosh.server.manger.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yosh.common.exception.BusinessException;
import com.yosh.common.exception.ErrorCode;
import com.yosh.common.exception.ThrowUtils;
import com.yosh.common.model.dto.file.UploadPictureResult;
import com.yosh.server.config.CosClientConfig;
import com.yosh.server.manger.CosManger;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private COSClient cosClient;
    @Resource
    private CosManger cosManger;

    /**
     * 上传文件
     * @param inputSource
     * @param prefix
     * @return
     */

    public UploadPictureResult uploadFile(Object  inputSource, String prefix) {
        //校验图片
        validPicture(inputSource);
        //上传地址
        String fileName = getOriginalFilename(inputSource);
        String filePath = prefix + fileName;
        String uuid = RandomUtil.randomString(10);
        //解析并返回
        String upFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, filePath);
        File tempFile = null;
        try {
            assert fileName != null;
            tempFile = File.createTempFile(fileName, null);
           //处理文件源
            processFile(inputSource, tempFile);
            PutObjectResult putObjectResult = cosManger.uploadFileAndGet(upFileName, tempFile);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            return UploadPictureResult.builder()
                    .url(cosManger.getObjectUrl(upFileName))
                    .picName(FileUtil.mainName(fileName))
                    .picSize(FileUtil.size(tempFile))
                    .picWidth( imageInfo.getWidth())
                    .picHeight( imageInfo.getHeight())
                    .picScale( imageInfo.getWidth() * 1.0 / imageInfo.getHeight())
                    .picFormat( imageInfo.getFormat())
                    .build();

        } catch (IOException e) {
            log.error("创建临时文件失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建临时文件失败");
        } finally {
            deleteTempFile(tempFile);
        }
    }

    protected  abstract  String getOriginalFilename(Object inputSource);

    protected abstract void processFile(Object inputSource, File tempFile);


    protected abstract void validPicture(Object inputResource);

    public void deleteTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
            log.warn("临时文件删除失败：{}", tempFile.getAbsolutePath());
        }
    }
}
