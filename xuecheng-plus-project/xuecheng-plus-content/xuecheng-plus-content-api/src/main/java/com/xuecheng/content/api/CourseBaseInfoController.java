package com.xuecheng.content.api;

import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfo;
import com.xuecheng.content.model.dto.EditCourse;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Api(value = "课程信息管理接口",tags = "课程信息管理接口")
@RestController
public class CourseBaseInfoController {

    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @ApiOperation("课程分页查询接口")
    @PostMapping("/course/list")
    public PageResult<com.xuecheng.content.model.po.CourseBase> list(PageParams pageParams, @RequestBody(required=false) QueryCourseParamsDto queryCourseParamsDto) {

        PageResult<com.xuecheng.content.model.po.CourseBase> courseBasePageResult = courseBaseInfoService.queryCourseBaseList(pageParams, queryCourseParamsDto);

        return courseBasePageResult;

    }

    @ApiOperation("新增课程")
    @PostMapping("/course")
    public CourseBaseInfo courseBaseInfoDto(@RequestBody @Validated(ValidationGroups.Insert.class) AddCourseDto addCourseDto){
        Long companyId=1232141425L;
        CourseBaseInfo courseBaseInfo = courseBaseInfoService.createCourseBase(companyId, addCourseDto);
        return courseBaseInfo;
    }

    @ApiOperation("根据课程id查询课程")
    @GetMapping("/course/{courseId}")
    public CourseBaseInfo getCourseBaseById(@PathVariable Long courseId){
        //获取当前用户的身份
//        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        CourseBaseInfo courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    @ApiOperation("修改课程")
    @PutMapping("/course")
    public CourseBaseInfo modifyCourseBase(@RequestBody @Validated(ValidationGroups.Update.class) EditCourse editCourseDto){
        //机构id，由于认证系统没有上线暂时硬编码
        Long companyId = 1232141425L;
        return courseBaseInfoService.updateCourseBase(companyId,editCourseDto);
    }

    @ApiOperation("删除课程")
    @DeleteMapping("/course/{courseId}")
    public void deleteCourse(@PathVariable Long courseId){
        courseBaseInfoService.deleteCourse(courseId);
    }

}
