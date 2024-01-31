package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.dto.AddCourseTeacherDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class CourseTeacherServiceImpl implements CourseTeacherService {
    @Autowired
    CourseTeacherMapper courseTeacherMapper;

    @Override
    public List<CourseTeacher> query(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId);
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(queryWrapper);
        return courseTeachers;
    }

    @Override
    public CourseTeacher updateTeacher(CourseTeacher courseTeacher) {
        Long id = courseTeacher.getId();
        if(id==null){
            if(StringUtils.isEmpty(courseTeacher.getTeacherName())){
                XueChengPlusException.cast("姓名不能为空");
            }
            if(StringUtils.isEmpty(courseTeacher.getPosition())){
                XueChengPlusException.cast("职位不能为空");
            }
            courseTeacher.setCreateDate(LocalDateTime.now());
            int insert = courseTeacherMapper.insert(courseTeacher);
            if (insert<=0){
                XueChengPlusException.cast("插入教师失败");
            }
        }
        else{
            int i = courseTeacherMapper.updateById(courseTeacher);
            if(i<=0){
                XueChengPlusException.cast("更新教师信息失败");
            }
        }
        CourseTeacher courseTeacherNew = courseTeacherMapper.selectById(courseTeacher);
        return courseTeacherNew;
    }

    @Override
    public void deleteTeacher(Long courseId, Long courseTeacherId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId).eq(CourseTeacher::getId,courseTeacherId);
        int delete = courseTeacherMapper.delete(queryWrapper);
        if(delete<=0){
            XueChengPlusException.cast("删除教师失败");
        }
    }
}
