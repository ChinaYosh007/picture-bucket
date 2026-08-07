package com.yosh.server.manger.upload;

import cn.hutool.core.io.FileUtil;
import com.yosh.common.exception.ErrorCode;
import com.yosh.common.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@Service
@Slf4j
public class FilePictureUpload extends PictureUploadTemplate{
    @Override
    protected String getOriginalFilename(Object inputSource) {
        return ((MultipartFile) inputSource).getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File tempFile) {
         try {
             ((MultipartFile) inputSource).transferTo(tempFile);
         } catch (Exception e) {
             log.error("创建临时文件失败");
             throw new RuntimeException(e);
         }
    }

    @Override
    protected void validPicture(Object inputResource) {
        ThrowUtils.throwIf(inputResource == null, ErrorCode.PARAMS_ERROR, "上传文件为空");
        // 校验文件大小
        MultipartFile file = (MultipartFile) inputResource;
        long size = file.getSize();
        final long ONE_MB = 1024 * 1024 * 3;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "上传文件过大");
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(file.getOriginalFilename());
        final List<String> SUFFIX_LIST = List.of("png", "jpg", "jpeg", "webp");
        ThrowUtils.throwIf(!SUFFIX_LIST.contains(suffix), ErrorCode.PARAMS_ERROR, "上传文件格式错误");
        log.info("上传文件格式正确");

    }
}
