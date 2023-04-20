package com.yupi.lovefinder.model.dto;

import com.yupi.lovefinder.common.PageRequest;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询请求
 *
 * @author yupi
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TagQueryRequest extends PageRequest implements Serializable {

    /**
     * 分类
     */
    private String category;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 创建用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}