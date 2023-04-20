package com.yupi.lovefinder.once;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.yupi.lovefinder.model.dto.PostImportDTO;
import com.yupi.lovefinder.model.entity.User;
import com.yupi.lovefinder.service.PostService;
import com.yupi.lovefinder.service.TagService;
import com.yupi.lovefinder.service.UserService;
import com.yupi.lovefinder.service.listener.PostImportListener;

import java.io.File;
import java.io.FileNotFoundException;
import javax.annotation.Resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

/**
 * 导入帖子（一次性任务）
 *
 * @author yupi
 */
@SpringBootTest
public class PostImportTest {

    @Resource
    private PostService postService;

    @Resource
    private TagService tagService;

    // 仅执行一次，之后可以注释掉 @Test 注解
    @Test
    public void doImport() throws FileNotFoundException {
        File file = ResourceUtils.getFile("classpath:初始数据.xlsx");
        EasyExcel.read(file, PostImportDTO.class, new PostImportListener(postService, tagService))
                .excelType(ExcelTypeEnum.XLSX)
                .sheet()
                .doRead();
    }

}