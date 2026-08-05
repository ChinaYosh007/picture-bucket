package com.yosh.server.controller;

import com.yosh.common.responese.BaseResponse;
import com.yosh.common.responese.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    /** 用于部署探活，不访问数据库或外部服务。 */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("success");
    }
}
