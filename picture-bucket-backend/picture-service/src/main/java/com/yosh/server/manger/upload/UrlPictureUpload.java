package com.yosh.server.manger.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.yosh.common.exception.BusinessException;
import com.yosh.common.exception.ErrorCode;
import com.yosh.common.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
@Service
@Slf4j
public class UrlPictureUpload extends PictureUploadTemplate{
    @Override
    protected String getOriginalFilename(Object inputSource) {
        return FileUtil.mainName( inputSource.toString());
    }

    @Override
    protected void processFile(Object inputSource, File tempFile) {
        try {
            HttpUtil.downloadFile(inputSource.toString(), tempFile);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载文件失败");
        }

    }

    @Override
    protected void validPicture(Object inputResource) {
        ThrowUtils.throwIf(inputResource == null, ErrorCode.PARAMS_ERROR, "上传文件为空");
        String url = inputResource.toString();
        ThrowUtils.throwIf(url == null, ErrorCode.PARAMS_ERROR, "文件地址为空");
        try{
            new URL(url);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"文件地址格式不正确");
        }
        ThrowUtils.throwIf(!url.startsWith("https://"), ErrorCode.SYSTEM_ERROR, "文件地址格式不支持");
        try(HttpResponse execute = HttpUtil.createRequest(Method.HEAD, url)
                .execute()) {
            if(execute.getStatus() != HttpStatus.HTTP_OK){
                return;
            }
            String header = execute.header("Content-Type");
            if(!StrUtil.isNotBlank( header)){
                final List<String> SUFFIX_LIST = List.of("image/png", "image/jpg", "image/jpeg", "image/webp");
                ThrowUtils.throwIf(!SUFFIX_LIST.contains(header), ErrorCode.SYSTEM_ERROR, "文件地址格式不支持");

            }
            header = execute.header("Content-Length");
            if(!StrUtil.isNotBlank( header)){
                final long ONE_MB = 1024 * 1024 * 3;
                long size = Long.parseLong(header);
                ThrowUtils.throwIf( size > ONE_MB, ErrorCode.SYSTEM_ERROR, "太大了人家受不了~");
            }

        }

    }
}
