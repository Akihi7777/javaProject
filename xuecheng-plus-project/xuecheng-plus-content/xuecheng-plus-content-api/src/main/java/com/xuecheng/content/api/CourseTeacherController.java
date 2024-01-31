package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.AddCourseTeacherDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CourseTeacherController {
    @Autowired
    CourseTeacherService courseTeacherService;

    @ApiOperation("查询教师")
    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> query(@PathVariable Long courseId){
        List<CourseTeacher> courseTeacherList = courseTeacherService.query(courseId);
        return courseTeacherList;
    }

    @ApiOperation("修改/新增教师")
    @PostMapping("/courseTeacher")
    public CourseTeacher updateTeacher(@RequestBody CourseTeacher courseTeacher){
        CourseTeacher courseTeacherNew = courseTeacherService.updateTeacher(courseTeacher);
        return courseTeacherNew;
    }

    @ApiOperation("删除教师")
    @DeleteMapping("/courseTeacher/course/{courseId}/{courseTeacherId}")
    public void deleteTeacher(@PathVariable("courseId") Long courseId,@PathVariable("courseTeacherId") Long courseTeacherId){
        courseTeacherService.deleteTeacher(courseId,courseTeacherId);
    }
}
