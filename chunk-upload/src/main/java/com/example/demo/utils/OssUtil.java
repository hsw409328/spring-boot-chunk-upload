package com.example.demo.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OSS工具包
 *
 * @author haoshuaiwei
 */
@Component
public class OssUtil {

    private AmazonS3 s3client;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String uploadIdSymbol = "XX:FILE:UPLOAD:ID:";
    private String partETagsSymbol = "XX:FILE:PART:ETAGS:";

    // 这些信息需要你购买了 oss才能够得到
    private String ak = "";
    private String sk = "";
    private String endpoint = "";
    private String region = "";
    private String buketName = "";

    @PostConstruct
    private void init() throws UnsupportedEncodingException {
        BasicAWSCredentials credentials = new BasicAWSCredentials(ak, sk);
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endpoint, region);
        s3client = AmazonS3ClientBuilder.standard().
                withCredentials(new AWSStaticCredentialsProvider(credentials)).
                enablePathStyleAccess().
                disableChunkedEncoding().
                withEndpointConfiguration(endpointConfiguration).
                build();
    }

    /**
     * 上初始化大文件上传环境，返回uploadId
     *
     * @param fileName 文件
     * @return 返回uploadId
     */
    public String bigFileInitUpload(String fileName) {
        // 初始化分片上传
        InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(buketName, fileName);
        InitiateMultipartUploadResult initiateMultipartUploadResult = s3client.initiateMultipartUpload(initiateMultipartUploadRequest);
        String uploadId = initiateMultipartUploadResult.getUploadId();
        //将uploadId缓存到redis,5个小时有效
        redisTemplate.opsForValue().set(uploadIdSymbol + uploadId, fileName, 60 * 60 * 5, TimeUnit.SECONDS);
        return uploadId;
    }

    /**
     * 上传指定的文件片断，返回uploadId
     *
     * @param uploadId 分片初始化标识
     * @param chunkId 文件ID
     * @param file 分片文件流
     * @return 文件MD5
     *
     */
    public String uploadChunk(String uploadId,
                              Integer chunkId,
                              MultipartFile file) throws IOException {
        InputStream inStream = file.getInputStream();
        long curPartSize = file.getSize();
        String objectName = String.valueOf(redisTemplate.opsForValue().get(uploadIdSymbol + uploadId));

        UploadPartRequest uploadPartRequest = new UploadPartRequest();
        uploadPartRequest.setBucketName(buketName);
        uploadPartRequest.setKey(objectName);
        uploadPartRequest.setUploadId(uploadId);
        uploadPartRequest.setInputStream(inStream);
        uploadPartRequest.setPartSize(curPartSize);
        // 设置分片号。每一个上传的分片都有一个分片号，取值范围是1~10000，如果超出这个范围，OSS将返回InvalidArgument的错误码。
        uploadPartRequest.setPartNumber(chunkId);
        // 每个分片不需要按顺序上传，甚至可以在不同客户端上传，OSS会按照分片号排序组成完整的文件。
        UploadPartResult uploadPartResult = s3client.uploadPart(uploadPartRequest);
        // 每次上传分片之后，OSS的返回结果包含PartETag。PartETag将被保存在partETags中。
        PartETag partETag = uploadPartResult.getPartETag();
        //需要将PartETag缓存存起来，如果是第一个块，需要先将创建list，以便add
        Object partETagListStr = redisTemplate.opsForValue().get(partETagsSymbol + uploadId);
        List<PartETag> partETagList = new ArrayList<PartETag>();
        if(!ObjectUtil.isEmpty(partETagListStr)){
            partETagList = JSONUtil.toList(JSONUtil.parseArray(partETagListStr), PartETag.class);
        }
        if (partETagList == null) {
            partETagList = new ArrayList<PartETag>();
        }
        partETagList.add(partETag);
        redisTemplate.opsForValue().set(partETagsSymbol + uploadId, JSONUtil.toJsonStr(partETagList), 60 * 60 * 5, TimeUnit.SECONDS);
        String md5Str = partETag.getETag();
        return md5Str;
    }

    /**
     * 合并文件，返回文件URL
     *
     * @param uploadId 分片ID
     * @return 返回文件返回URL
     */
    public String completeFile(String uploadId) throws JsonProcessingException {
        //获取该uploadId缓存的信息和各块的信息，
        String objectName = String.valueOf( redisTemplate.opsForValue().get(uploadIdSymbol + uploadId));
        String partETagListStr = String.valueOf( redisTemplate.opsForValue().get(partETagsSymbol + uploadId));
        List<PartETag> partETagList = JSONUtil.toList(JSONUtil.parseArray(partETagListStr), PartETag.class);
        // OSS会自动去合并partEtag分片
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(buketName, objectName, uploadId, partETagList);

        // 完成合并，并返回结果
        CompleteMultipartUploadResult completeMultipartUploadResult = s3client.completeMultipartUpload(completeMultipartUploadRequest);

        redisTemplate.delete(uploadIdSymbol + uploadId);
        redisTemplate.delete(partETagsSymbol + uploadId);
        return completeMultipartUploadResult.getLocation();
    }



}
