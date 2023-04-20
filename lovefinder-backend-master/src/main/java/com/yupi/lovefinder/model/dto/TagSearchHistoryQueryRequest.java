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
public class TagSearchHistoryQueryRequest extends PageRequest implements Serializable {

    /**
     * 筛选标签名称
     */
    private String tagName;

    private static final long serialVersionUID = 1L;
}