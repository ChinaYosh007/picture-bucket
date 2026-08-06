package com.yosh.server.manger;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.OriginalInfo;
import com.yosh.common.exception.BusinessException;
import com.yosh.common.exception.ErrorCode;
import com.yosh.common.exception.ThrowUtils;
import com.yosh.common.model.dto.file.UploadPictureResult;
import com.yosh.common.responese.ResultUtils;
import com.yosh.server.config.CosClientConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class FileManger {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private COSClient cosClient;
    @Resource
    private CosManger cosManger;

    /**
     * 上传文件
     * @param file
     * @return
     */

    public UploadPictureResult uploadFile(MultipartFile file, String prefix) {
        //校验图片
        validPicture(file);
        //上传地址
        String fileName = file.getOriginalFilename();
        String filePath = prefix + fileName;
        String uuid = RandomUtil.randomString(10);
        //解析并返回
        String upFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, filePath);
        File tempFile = null;
        try {
            assert fileName != null;
            tempFile = File.createTempFile(fileName, null);
            file.transferTo(tempFile);
            PutObjectResult putObjectResult = cosManger.uploadFileAndGet(upFileName, tempFile);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            return UploadPictureResult.builder()
                     .url(cosManger.getObjectUrl(upFileName))
                     .picName(FileUtil.mainName(fileName))
                     .picSize(file.getSize())
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


    private void validPicture(MultipartFile file) {
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "上传文件为空");
        // 校验文件大小
        long size = file.getSize();
        final long ONE_MB = 1024 * 1024 * 3;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "上传文件过大");
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(file.getOriginalFilename());
        final List<String> SUFFIX_LIST = List.of("png", "jpg", "jpeg", "webp");
        ThrowUtils.throwIf(!SUFFIX_LIST.contains(suffix), ErrorCode.PARAMS_ERROR, "上传文件格式错误");
        log.info("上传文件格式正确");
    }

    public void deleteTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
            log.warn("临时文件删除失败：{}", tempFile.getAbsolutePath());
        }
    }
}
