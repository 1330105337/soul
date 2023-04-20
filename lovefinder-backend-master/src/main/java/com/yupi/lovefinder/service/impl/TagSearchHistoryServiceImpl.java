package com.yupi.lovefinder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.lovefinder.common.ErrorCode;
import com.yupi.lovefinder.exception.BusinessException;
import com.yupi.lovefinder.mapper.TagSearchHistoryMapper;
import com.yupi.lovefinder.model.entity.TagSearchHistory;
import com.yupi.lovefinder.service.TagSearchHistoryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author yupili
 * @description 针对表【tag_search_history(标签筛选记录)】的数据库操作Service实现
 */
@Service
public class TagSearchHistoryServiceImpl extends ServiceImpl<TagSearchHistoryMapper, TagSearchHistory>
        implements TagSearchHistoryService {

    @Override
    public boolean addTagSearchHistory(String tagName) {
        if (StringUtils.isEmpty(tagName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<TagSearchHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tagName", tagName);
        synchronized (tagName.intern()) {
            TagSearchHistory tagSearchHistory = this.getOne(queryWrapper);
            // 无记录则插入
            if (tagSearchHistory == null) {
                tagSearchHistory = new TagSearchHistory();
                tagSearchHistory.setTagName(tagName);
                tagSearchHistory.setNum(1);
                return this.save(tagSearchHistory);
            } else {
                // 有记录则次数 + 1
                return this.lambdaUpdate()
                        .eq(TagSearchHistory::getTagName, tagName)
                        .setSql("num = num + 1")
                        .update();
            }
        }
    }
}




