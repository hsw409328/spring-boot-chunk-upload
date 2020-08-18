package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件返回类
 *
 * @author haoshuaiwei
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileVo {

    private Integer status;
    private String filePath;

}
