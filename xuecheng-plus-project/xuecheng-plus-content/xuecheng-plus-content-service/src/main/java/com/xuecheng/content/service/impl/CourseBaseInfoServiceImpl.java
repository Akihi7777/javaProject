package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfo;
import com.xuecheng.content.model.dto.EditCourse;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {
    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;

    @Autowired
    CourseTeacherMapper courseTeacherMapper;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto courseParamsDto) {

        //拼装查询条件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //根据名称模糊查询,在sql中拼接 course_base.name like '%值%'
        queryWrapper.like(StringUtils.isNotEmpty(courseParamsDto.getCourseName()), CourseBase::getName,courseParamsDto.getCourseName());
        //根据课程审核状态查询 course_base.audit_status = ?
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getAuditStatus()), CourseBase::getAuditStatus,courseParamsDto.getAuditStatus());
        //按课程发布状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getPublishStatus()), CourseBase::getStatus,courseParamsDto.getPublishStatus());
        //创建page分页参数对象，参数：当前页码，每页记录数
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        //开始进行分页查询
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        //数据列表
        List<CourseBase> items = pageResult.getRecords();
        //总记录数
        long total = pageResult.getTotal();

        //List<T> items, long counts, long page, long pageSize
        PageResult<CourseBase> courseBasePageResult = new PageResult<CourseBase>(items,total,pageParams.getPageNo(), pageParams.getPageSize());
        return  courseBasePageResult;
    }

    //增删改需要标记@Transactional，进行事务管理，在崩溃时，可以进行回滚操作
    @Transactional
    @Override
    public CourseBaseInfo createCourseBase(Long companyId, AddCourseDto dto) {
        //合法性校验
        if (StringUtils.isBlank(dto.getName())) {
            throw new RuntimeException("课程名称为空");
        }

        if (StringUtils.isBlank(dto.getMt())) {
            throw new RuntimeException("课程分类为空");
        }

        if (StringUtils.isBlank(dto.getSt())) {
            throw new RuntimeException("课程分类为空");
        }

        if (StringUtils.isBlank(dto.getGrade())) {
            throw new RuntimeException("课程等级为空");
        }

        if (StringUtils.isBlank(dto.getTeachmode())) {
            throw new RuntimeException("教育模式为空");
        }

        if (StringUtils.isBlank(dto.getUsers())) {
            throw new RuntimeException("适应人群为空");
        }

        if (StringUtils.isBlank(dto.getCharge())) {
            throw new RuntimeException("收费规则为空");
        }

        CourseBase courseBaseNew = new CourseBase();

        BeanUtils.copyProperties(dto,courseBaseNew);
        courseBaseNew.setCompanyId(companyId);
        courseBaseNew.setCreateDate(LocalDateTime.now());
        //审核状态默认为未提交
        courseBaseNew.setAuditStatus("200202");
        //发布状态未发布
        courseBaseNew.setStatus("203202");

        int insert=courseBaseMapper.insert(courseBaseNew);
        if(insert<=0){
            throw new RuntimeException("插入课程信息失败");
        }

        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(dto,courseMarket);

        Long courseId=courseBaseNew.getId();
        courseMarket.setId(courseId);

        //保存营销信息
        saveCourseMarket(courseMarket);

        //返回新增的信息
        CourseBaseInfo courseBaseInfo = getCourseBaseInfo(courseId);

        return courseBaseInfo;
    }

    //根据课程id查询课程基本信息，包括基本信息和营销信息
    public CourseBaseInfo getCourseBaseInfo(Long courseId){

        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase == null){
            return null;
        }
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        CourseBaseInfo courseBaseInfoInfoDto = new CourseBaseInfo();
        BeanUtils.copyProperties(courseBase, courseBaseInfoInfoDto);
        if(courseMarket != null){
            BeanUtils.copyProperties(courseMarket, courseBaseInfoInfoDto);
        }

        //查询分类名称
        CourseCategory courseCategoryBySt = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoInfoDto.setStName(courseCategoryBySt.getName());
        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoInfoDto.setMtName(courseCategoryByMt.getName());

        return courseBaseInfoInfoDto;
    }

    @Override
    public CourseBaseInfo updateCourseBase(Long companyId, EditCourse editCourse) {
        Long courseId = editCourse.getId();
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if(courseBase==null){
            XueChengPlusException.cast("课程不存在");
        }
        //数据合法性校验
        //本机构只能修改本机构的课程
        if(!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("本机构只能修改本机构的课程");
        }

        //封装数据
        BeanUtils.copyProperties(editCourse,courseBase);

        courseBase.setChangeDate(LocalDateTime.now());

        //更新数据库
        int insert = courseBaseMapper.updateById(courseBase);
        if(insert<=0){
            XueChengPlusException.cast("修改课程失败");
        }
        int update;
        //更新营销信息
        if(courseMarket!=null){
            BeanUtils.copyProperties(editCourse,courseMarket);
            update = saveCourseMarket(courseMarket);
        }
        else {
            CourseMarket courseMarket_n = new CourseMarket();
            BeanUtils.copyProperties(editCourse,courseMarket_n);
            update = saveCourseMarket(courseMarket_n);
        }
        if(update<=0){
            XueChengPlusException.cast("修改营销信息失败");
        }

        CourseBaseInfo courseBaseInfo = getCourseBaseInfo(courseBase.getId());
        return courseBaseInfo;
    }

    @Override
    public void deleteCourse(Long courseId) {
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase.getAuditStatus().equals("202002")){
            //删除基本信息
            courseBaseMapper.deleteById(courseId);
            //删除营销信息
            CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
            if(courseMarket!=null){
                courseMarketMapper.deleteById(courseId);
            }
            //删除课程计划
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getCourseId,courseId).select(Teachplan::getId);
            teachplanMapper.delete(queryWrapper);
            //删除关联的媒资信息
            LambdaQueryWrapper<TeachplanMedia> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(TeachplanMedia::getCourseId,courseId);
            teachplanMediaMapper.delete(queryWrapper1);
            //删除教师信息
            LambdaQueryWrapper<CourseTeacher> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.eq(CourseTeacher::getCourseId,courseId);
            courseTeacherMapper.delete(queryWrapper2);
        }
        else{
            XueChengPlusException.cast("只能删除为提交审核的课程");
        }
    }


    //单独写一个方法㝍营销信息，存在即更新，不存在就添加
    private int saveCourseMarket(CourseMarket courseMarketNew){
        //合法性校验
        String charge = courseMarketNew.getCharge();
        if(StringUtils.isBlank(charge)){
            throw new RuntimeException("收费规则没有选择");
        }
        //如果课程收费，价格没有填写也需要抛出异常
        if(charge.equals("201001")){
            if(courseMarketNew.getPrice()==null||courseMarketNew.getPrice().floatValue()<=0||courseMarketNew.getOriginalPrice()<=0){
                XueChengPlusException.cast("课程价格不能为空并且必须大于0");
            }
        }

        Long id=courseMarketNew.getId();
        CourseMarket courseMarket = courseMarketMapper.selectById(id);
        if(courseMarket==null){
            int insert = courseMarketMapper.insert(courseMarketNew);
            return insert;
        }
        else{
            BeanUtils.copyProperties(courseMarketNew,courseMarket);
            courseMarket.setId(id);
            int update=courseMarketMapper.updateById(courseMarket);
            return update;
        }
    }


}
