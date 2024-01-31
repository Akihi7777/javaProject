package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.AddCourseTeacherDto;
import com.xuecheng.content.model.po.CourseTeacher;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface CourseTeacherService {
    public List<CourseTeacher> query(Long courseId);

    public CourseTeacher updateTeacher(CourseTeacher courseTeacher);

    public void deleteTeacher(Long courseId,Long courseTeacherId);
}
