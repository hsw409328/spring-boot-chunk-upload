package com.example.demo.controller;

import com.example.demo.utils.OssUtil;
import com.example.demo.vo.FileVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 主控制器
 * @author haoshuaiwei
 */
@RestController
@RequestMapping("/file")
public class MainController {

    @Autowired
    private OssUtil ossUtil;

    /**
     * 大文件上传初始化
     * 返回文件uploadId
     */
    @PostMapping("/big/init")
    public String initBigFileUpload(@RequestParam(name = "fileName") String fileName) {
        String uploadId = ossUtil.bigFileInitUpload(fileName);
        return uploadId;
    }

    /**
     * 上传大文件的Chunk
     * 返回chunk的MD5
     */
    @PostMapping("/big/chunk")
    public String uploadChunk(@RequestParam("file") MultipartFile chunkFile,
                              @RequestParam(name = "uploadId", required = true) String uploadId,
                              @RequestParam(name = "chunkNumber", required = true) Integer chunkId) throws IOException {

        String md5Str = ossUtil.uploadChunk(uploadId, chunkId, chunkFile);
        return md5Str;
    }

    /**
     * 大文件上传完成后合并
     * 返回文件访问的URL
     */
    @PostMapping("/big/merge")
    public FileVo mergeFile( @RequestParam(name = "uploadId", required = true) String uploadId) throws JsonProcessingException {
        String url = ossUtil.completeFile(uploadId);
        FileVo fileVo = new FileVo();
        fileVo.setStatus(200);
        fileVo.setFilePath(url);
        return fileVo;
    }


}
