package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignclient.CourseIndex;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.mapper.MqMessageMapper;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {
    @Autowired
    CoursePublishService coursePublishService;

    @Autowired
    SearchServiceClient searchServiceClient;

    @Autowired
    CoursePublishMapper coursePublishMapper;

    //任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception{
        //分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        //调用抽象类的方法执行任务
        process(shardIndex,shardTotal,"course_publish",30,60);
    }

    @Override
    public boolean execute(MqMessage mqMessage) {
        //获取消息相关的业务信息
        String businessKey1 = mqMessage.getBusinessKey1();
        long courseId = Integer.parseInt(businessKey1);
        //课程静态化
        generateCourseHtml(mqMessage,courseId);
        //课程索引
        saveCourseIndex(mqMessage,courseId);
        //课程缓存
        saveCourseCache(mqMessage,courseId);
        return true;

    }

    private void generateCourseHtml(MqMessage mqMessage,long courseId){
        MqMessageService mqMessageService = this.getMqMessageService();
        Long taskId = mqMessage.getId();
        int stageOne = mqMessageService.getStageOne(taskId);
        //任务已执行
        if(stageOne>0){
            log.debug("课程静态化处理已完成，无需处理");
            return;
        }
        //开始课程静态化处理
        File file = coursePublishService.generateCourseHtml(courseId);
        if(file==null){
            XueChengPlusException.cast("生成的静态页面为空");
        }
        coursePublishService.uploadCourseHtml(courseId,file);
        mqMessageService.completedStageOne(taskId);
    }

    private void saveCourseIndex(MqMessage mqMessage,long courseId){
        MqMessageService mqMessageService = this.getMqMessageService();
        Long taskId = mqMessage.getId();
        //取出第二个阶段状态
        int stageTwo = mqMessageService.getStageTwo(taskId);
        if(stageTwo>0){
            log.debug("课程索引信息已写入，无需处理");
            return;
        }
        //查询课程信息（发布表），调用搜索服务
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);
        //远程调用
        Boolean add = searchServiceClient.add(courseIndex);
        if(!add){
            XueChengPlusException.cast("远程调用添加索引失败");
        }
        mqMessageService.completedStageTwo(taskId);
    }

    private void saveCourseCache(MqMessage mqMessage,long courseId){
        MqMessageService mqMessageService = this.getMqMessageService();
        Long taskId = mqMessage.getId();
        //取出第三个阶段状态
        int stageThree= mqMessageService.getStageThree(taskId);
        if(stageThree>0){
            log.debug("课程缓存已写入，无需处理");
            return;
        }
        //写入课程缓存

        mqMessageService.completedStageThree(taskId);
    }
}
