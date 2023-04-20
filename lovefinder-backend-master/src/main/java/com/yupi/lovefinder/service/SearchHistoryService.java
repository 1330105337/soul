package com.yupi.lovefinder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.lovefinder.model.entity.SearchHistory;

/**
* @author yupili
* @description 针对表【search_history(搜索记录)】的数据库操作Service
*/
public interface SearchHistoryService extends IService<SearchHistory> {

    /**
     * 添加搜索记录
     *
     * @param word
     * @return
     */
    boolean addSearchHistory(String word);
}
