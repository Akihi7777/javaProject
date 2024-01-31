package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseTeacher;
import lombok.Data;

import java.util.List;

@Data
public class CoursePreviewDto {
    //课程基本信息 营销信息
    private CourseBaseInfo courseBaseInfo;
    //课程计划信息
    private List<TeachplanDto> teachplans;
    //师资信息
    private List<CourseTeacher> courseTeacher;
}
