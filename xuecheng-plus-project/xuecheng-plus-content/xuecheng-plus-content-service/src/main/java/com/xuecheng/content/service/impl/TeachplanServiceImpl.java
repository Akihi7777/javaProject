package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TeachplanServiceImpl implements TeachplanService {
    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;
    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);
        return teachplanDtos;
    }

    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        Long id = saveTeachplanDto.getId();
        if(id==null){
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            //最大order的获取
            Long courseId = teachplan.getCourseId();
            Long parentid = teachplan.getParentid();
            Integer maxOrder = getMaxOrder(parentid,courseId);
            if(maxOrder==null){
                teachplan.setOrderby(1);
            }
            else {
                teachplan.setOrderby(maxOrder.intValue() + 1);
            }
            int insert = teachplanMapper.insert(teachplan);
            if(insert<=0){
                XueChengPlusException.cast("添加课程计划失败");
            }
        }
        else {
            Teachplan teachplan = teachplanMapper.selectById(saveTeachplanDto.getId());
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            int update = teachplanMapper.updateById(teachplan);
            if(update<=0){
                XueChengPlusException.cast("修改课程计划失败");
            }
        }
    }

    @Override
    public void deleteTeachplan(Long teachplanId) {
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        Integer grade = teachplan.getGrade();
        if(grade.intValue()>1){
            int i = teachplanMapper.deleteById(teachplanId);
            if(i<=0){
                XueChengPlusException.cast("删除课程计划失败");
            }
            //删除相关媒介，先判断是否有相关的媒介
            //QueryWrapper queryWrapper = new QueryWrapper();
            QueryWrapper<TeachplanMedia> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("teachplan_id",teachplanId);
            TeachplanMedia teachplanMedia = teachplanMediaMapper.selectOne(queryWrapper);
            if(teachplanMedia!=null){
                int i1 = teachplanMediaMapper.delete(queryWrapper);
                if(i1<=0){
                    XueChengPlusException.cast("删除课程媒资失败");
                }
            }
        }
        else{
            List<Teachplan> teachplans = teachplanMapper.selectChildren(teachplanId);
            if(!teachplans.isEmpty()){
                XueChengPlusException.cast("课程计划信息还有子级信息，无法操作");
            }
            else {
                int i = teachplanMapper.deleteById(teachplanId);
                if(i<=0){
                    XueChengPlusException.cast("删除课程计划失败");
                }
            }
        }
    }

    @Override
    public void moveTeachplan(String moveType, Long teachplanId) {
        if(moveType.equals("movedown")){
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            //找出在当前计划下方的计划
            Long parentid = teachplan.getParentid();
            Long courseId = teachplan.getCourseId();
            Integer orderby = teachplan.getOrderby();
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getParentid,parentid).eq(Teachplan::getCourseId,courseId).gt(Teachplan::getOrderby,orderby).orderByAsc(Teachplan::getOrderby).last("limit 1");
            Teachplan teachplanDown = teachplanMapper.selectOne(queryWrapper);
            if(teachplanDown==null){
                XueChengPlusException.cast("已经到底啦");
            }
            //开始移动
            //交换两个计划的orderby
            Integer orderbyDown = teachplanDown.getOrderby();
            Integer temp=orderby;
            orderby=orderbyDown;
            orderbyDown=temp;
            teachplan.setOrderby(orderby);
            teachplanDown.setOrderby(orderbyDown);
            int i = teachplanMapper.updateById(teachplan);
            if(i<=0){
                XueChengPlusException.cast("移动课程计划失败");
            }
            int i1 = teachplanMapper.updateById(teachplanDown);
            if(i1<=0){
                XueChengPlusException.cast("移动课程计划失败");
            }
        }
        else {
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            //找出在当前计划上方的计划
            Long parentid = teachplan.getParentid();
            Long courseId = teachplan.getCourseId();
            Integer orderby = teachplan.getOrderby();
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getParentid,parentid).eq(Teachplan::getCourseId,courseId).lt(Teachplan::getOrderby,orderby).orderByDesc(Teachplan::getOrderby).last("limit 1");
            Teachplan teachplanUp = teachplanMapper.selectOne(queryWrapper);
            if(teachplanUp==null){
                XueChengPlusException.cast("已经到顶啦");
            }
            //开始移动
            //交换两个计划的orderby
            Integer orderbyUp = teachplanUp.getOrderby();
            Integer temp=orderby;
            orderby=orderbyUp;
            orderbyUp=temp;
            teachplan.setOrderby(orderby);
            teachplanUp.setOrderby(orderbyUp);
            int i = teachplanMapper.updateById(teachplan);
            if(i<=0){
                XueChengPlusException.cast("移动课程计划失败");
            }
            int i1 = teachplanMapper.updateById(teachplanUp);
            if(i1<=0){
                XueChengPlusException.cast("移动课程计划失败");
            }
        }
    }

    @Transactional
    @Override
    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
        //先删除原有的记录
        LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeachplanMedia::getTeachplanId,bindTeachplanMediaDto.getTeachplanId());
        teachplanMediaMapper.delete(queryWrapper);
        //再添加新的记录
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        BeanUtils.copyProperties(bindTeachplanMediaDto,teachplanMedia);
        teachplanMedia.setCreateDate(LocalDateTime.now());
        Teachplan teachplan = teachplanMapper.selectById(bindTeachplanMediaDto.getTeachplanId());
        if(teachplan==null){
            XueChengPlusException.cast("不存在对应的教学计划！");
        }
        Long courseId = teachplan.getCourseId();
        teachplanMedia.setCourseId(courseId);
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        int insert = teachplanMediaMapper.insert(teachplanMedia);
        if(insert<=0){
            XueChengPlusException.cast("添加媒资信息失败");
        }
    }

    //获取当前最大排序字段
    private Integer getMaxOrder(Long parentid,Long courseId ){
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getParentid,parentid).eq(Teachplan::getCourseId,courseId).select(Teachplan::getOrderby).orderByDesc(Teachplan::getOrderby).last("LIMIT 1");
        Teachplan teachplan = teachplanMapper.selectOne(queryWrapper);
        return teachplan != null ? teachplan.getOrderby() : null;
    }
}
