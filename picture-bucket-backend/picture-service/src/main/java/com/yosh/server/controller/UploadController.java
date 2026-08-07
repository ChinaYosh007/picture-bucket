package com.yosh.server.controller;

import com.yosh.common.constants.UserConstant;
import com.yosh.common.exception.BusinessException;
import com.yosh.common.exception.ErrorCode;
import com.yosh.common.model.dto.file.UploadPictureResult;
import com.yosh.common.model.dto.picture.PictureUploadRequest;
import com.yosh.common.responese.BaseResponse;
import com.yosh.common.responese.ResultUtils;
import com.yosh.server.annotation.AuthCheck;
import com.yosh.server.manger.CosManger;
import com.yosh.server.manger.FileManger;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/file")
@Slf4j
public class UploadController {

    @Resource
    private FileManger fileManger;
    @Resource
    private CosManger cosManger;

    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @ModelAttribute PictureUploadRequest uploadRequest,   // ← 这行关键
            HttpServletRequest request) {                         // 保留原有的 request 参数

        UploadPictureResult uploadPictureResult = fileManger.uploadFile(file, "public/");
        return ResultUtils.success(uploadPictureResult.getUrl());
    }

    @PostMapping("/download")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> downloadFile(@RequestParam("fileName") String fileName) {
        return ResultUtils.success(cosManger.getObjectUrl(fileName));
    }
}
