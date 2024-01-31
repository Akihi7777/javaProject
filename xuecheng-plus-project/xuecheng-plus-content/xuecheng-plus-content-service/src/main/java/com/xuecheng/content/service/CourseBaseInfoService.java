package com.xuecheng.content.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfo;
import com.xuecheng.content.model.dto.EditCourse;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;

/**
 * @author Mr.M
 * @version 1.0
 * @description 课程信息管理接口
 * @date 2023/2/12 10:14
 */
public interface CourseBaseInfoService {

    /**
     * 课程分页查询
     * @param pageParams 分页查询参数
     * @param courseParamsDto 查询条件
     * @return 查询结果
     */
    public PageResult<com.xuecheng.content.model.po.CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto courseParamsDto);

    public CourseBaseInfo createCourseBase(Long companyId, AddCourseDto addCourseDto);

    public CourseBaseInfo getCourseBaseInfo(Long courseId);

    public CourseBaseInfo updateCourseBase(Long companyId, EditCourse editCourse);

    public void deleteCourse(Long courseId);
}
