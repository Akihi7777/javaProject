package com.xuecheng.content;

import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TeachPlanMapperTests {
    @Autowired
    TeachplanMapper teachplanMapper;
    TeachplanMediaMapper teachplanMediaMapper;

    @Test
    public void testteachplanMapper(){
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(117L);
        System.out.println(teachplanDtos);
    }

    @Test
    public void selectTeachplanChildren(){
        List<Teachplan> teachplans = teachplanMapper.selectChildren(43L);
        System.out.println(teachplans);
    }
}
