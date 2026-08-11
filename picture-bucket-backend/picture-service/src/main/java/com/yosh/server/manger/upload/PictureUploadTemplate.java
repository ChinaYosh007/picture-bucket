package com.yosh.server.manger.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
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

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp", "gif");

    /**
     * 上传文件
     * @param inputSource
     * @param prefix
     * @return
     */

    public UploadPictureResult uploadFile(Object inputSource, String prefix) {
        // 校验图片
        validPicture(inputSource);
        // 原始文件名
        String fileName = getOriginalFilename(inputSource);
        File tempFile = null;
        try {
            tempFile = File.createTempFile("upload_", ".tmp");
            // 处理文件源
            processFile(inputSource, tempFile);

            // 获取文件后缀
            String suffix = FileUtil.getSuffix(fileName);
            if (StrUtil.isBlank(suffix) || !ALLOWED_EXTENSIONS.contains(suffix.toLowerCase())) {
                suffix = FileUtil.getType(tempFile);
            }
            if (StrUtil.isBlank(suffix) || !ALLOWED_EXTENSIONS.contains(suffix.toLowerCase())) {
                suffix = "jpg";
            }

            // 构造上传到 COS 的存储路径：prefix/2026-08-08_uuid.jpg
            String formattedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
            String uuid = RandomUtil.randomString(10);
            String upFileName = String.format("%s%s_%s.%s", formattedPrefix, DateUtil.formatDate(new Date()), uuid, suffix);

            PutObjectResult putObjectResult = cosManger.uploadFileAndGet(upFileName, tempFile);
            // 获取压缩图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if(CollUtil.isNotEmpty(objectList)){
                // 压缩
               CIObject ciObject = objectList.getFirst();
                CIObject thumbnail = ciObject;
               //缩略图
                if( objectList.size() > 1){
                    thumbnail = objectList.get(1);
                }

               return UploadPictureResult.builder()
                       .url(cosManger.getObjectUrl(upFileName))
                       .picName(ciObject.getKey())
                       .picSize(FileUtil.size(tempFile))
                       .picWidth(ciObject.getWidth())
                       .picHeight(ciObject.getHeight())
                       .picScale(ciObject.getWidth() * 1.0 / ciObject.getHeight())
                       .picFormat(ciObject.getFormat())
                       .thumbnailUrl(cosClientConfig.getHost() + "/" + thumbnail.getKey())
                       .build();
            }
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            String mainName = FileUtil.mainName(fileName);
            if (StrUtil.isBlank(mainName)) {
                mainName = uuid;
            }
            String format = (imageInfo != null && StrUtil.isNotBlank(imageInfo.getFormat())) ? imageInfo.getFormat() : suffix;

            return UploadPictureResult.builder()
                    .url(cosManger.getObjectUrl(upFileName))
                    .picName(mainName)
                    .picSize(FileUtil.size(tempFile))
                    .picWidth(imageInfo != null ? imageInfo.getWidth() : 0)
                    .picHeight(imageInfo != null ? imageInfo.getHeight() : 0)
                    .picScale(imageInfo != null && imageInfo.getHeight() > 0 ? imageInfo.getWidth() * 1.0 / imageInfo.getHeight() : 0.0)
                    .picFormat(format)
                    .build();

        } catch (IOException e) {
            log.error("创建临时文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建临时文件失败");
        } finally {
            deleteTempFile(tempFile);
        }
    }

    protected abstract String getOriginalFilename(Object inputSource);

    protected abstract void processFile(Object inputSource, File tempFile);

    protected abstract void validPicture(Object inputResource);

    public void deleteTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
            log.warn("临时文件删除失败：{}", tempFile.getAbsolutePath());
        }
    }
}