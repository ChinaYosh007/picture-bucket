package com.yosh.server.controller;

import com.yosh.common.responese.BaseResponse;
import com.yosh.common.responese.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    public static final String URL = "http://localhost:8080/api/swagger-ui.html";
    @GetMapping("/health")
    public BaseResponse health() {
        return ResultUtils.success("success");
    }
}