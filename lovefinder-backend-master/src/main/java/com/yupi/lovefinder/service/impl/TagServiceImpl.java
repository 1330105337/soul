package com.yupi.lovefinder.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.lovefinder.model.entity.Tag;
import com.yupi.lovefinder.service.TagService;
import com.yupi.lovefinder.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author yupili
* @description 针对表【tag(标签)】的数据库操作Service实现
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




