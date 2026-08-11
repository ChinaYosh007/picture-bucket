package com.yosh.server.manger;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.yosh.server.config.CosClientConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManger {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private COSClient cosClient;
    /**
     * delete
     */
    public void deleteFile(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

    /**
     * 上传文件
     *
     * @param fileName
     * @param file
     * @return
     */
    public PutObjectResult uploadFile(String fileName, File file) {
        PutObjectRequest put = new PutObjectRequest(cosClientConfig.getBucket(), fileName, file);
        return cosClient.putObject(put);
    }

    /**
     * 获取文件访问地址
     *
     * @param key
     * @return
     */
    public String getObjectUrl(String key) {
        return cosClient.getObjectUrl(cosClientConfig.getBucket(), key).toString();
    }

    /**
     * 上传并且解析文件
     *
     * @param fileName
     * @param file
     */
    public PutObjectResult uploadFileAndGet(String fileName, File file) {
        PutObjectRequest put = new PutObjectRequest(cosClientConfig.getBucket(), fileName, file);
        PicOperations picOperations = new PicOperations();
        String webKey = FileUtil.mainName(fileName) + ".webp";
        PicOperations.Rule rule = new PicOperations.Rule();
        rule.setFileId(webKey);
        rule.setRule("imageMogr2/format/webp");
        rule.setBucket(cosClientConfig.getBucket());
        rule.setFileId(webKey);
        List<PicOperations.Rule> rules = new ArrayList<>();
        rules.add(rule);
        PicOperations.Rule thumbnailRule = new PicOperations.Rule();
        // 设置图片处理参数
        if(file.length() > 2048){
            String thumbnailKey = FileUtil.mainName(fileName) + "_thumbnail.webp";
            thumbnailRule.setFileId(thumbnailKey);
            // 缩放规则 /thumbnail/<Width>x<Height>>（如果大于原图宽高，则不处理）
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            rules.add(thumbnailRule);
        }

        // 构造处理参数
        picOperations.setRules(rules);
        put.setPicOperations(picOperations);
        return cosClient.putObject(put);
    }
}