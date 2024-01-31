package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.CourseBaseInfo;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.CourseTeacherService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.content.service.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import freemarker.template.Configuration;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CoursePublishServiceImpl implements CoursePublishService {
    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Autowired
    TeachplanService teachplanService;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseTeacherService courseTeacherService;

    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CoursePublishMapper coursePublishMapper;

    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    CoursePublishService coursePublishService;

    @Autowired
    MediaServiceClient mediaServiceClient;


    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        //课程基本信息、营销信息
        CourseBaseInfo courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);

        //课程计划信息
        List<TeachplanDto> teachplanTree= teachplanService.findTeachplanTree(courseId);

        List<CourseTeacher> courseTeacherList = courseTeacherService.query(courseId);

        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBaseInfo(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplanTree);
        coursePreviewDto.setCourseTeacher(courseTeacherList);
        return coursePreviewDto;

    }

    @Transactional
    @Override
    public void commitAudit(Long companyId, Long courseId) {
        //约束判断
        CourseBaseInfo courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if(courseBaseInfo==null){
            XueChengPlusException.cast("课程不存在！");
        }
        String auditStatus = courseBaseInfo.getAuditStatus();
        if(auditStatus.equals("202003")){
            XueChengPlusException.cast("课程已提交，请等待审核！");
        }
        String pic = courseBaseInfo.getPic();
        if(StringUtils.isEmpty(pic)){
            XueChengPlusException.cast("请上传课程图片");
        }
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if(teachplanTree==null||teachplanTree.size()<=0){
            XueChengPlusException.cast("请上传目录");
        }
        if(!companyId.equals(courseBaseInfo.getCompanyId())){
            XueChengPlusException.cast("只能提交本机构的课程");
        }
        //将相关信息插入课程预发布表
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfo,coursePublishPre);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //转json格式
        String courseMarketJson = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarketJson);
        String teachplanTreeJson = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachplanTreeJson);
        coursePublishPre.setStatus("202003");
        coursePublishPre.setCreateDate(LocalDateTime.now());
        //coursePublishPre.setCompanyId(companyId);
        //查询预发布表，有则插入，无则更新
        CoursePublishPre coursePublishPreDB = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPreDB==null){
            coursePublishPreMapper.insert(coursePublishPre);
        }
        else {
            coursePublishPreMapper.updateById(coursePublishPreDB);
        }
        //更新基本信息表的状态为提交
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
    }

    @Transactional
    @Override
    public void publish(Long companyId, Long courseId) {
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre==null){
            XueChengPlusException.cast("课程未提交审核");
        }
        if(!coursePublishPre.getStatus().equals("202004")){
            XueChengPlusException.cast("课程未通过审核");
        }
        //向发布表写入数据
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre,coursePublish);
        CoursePublish coursePublishDB = coursePublishMapper.selectById(courseId);
        if(coursePublishDB==null){
            coursePublishMapper.insert(coursePublish);
        }
        else {
            coursePublishMapper.updateById(coursePublish);
        }
        //向消息表写入数据
        saveCoursePublishMessage(courseId);
        //将预发布表删除
        int i = coursePublishPreMapper.deleteById(coursePublishPre);
        if(i<=0){
            XueChengPlusException.cast("删除预发布表失败");
        }
    }

    @Override
    public File generateCourseHtml(Long courseId) {
        //配置freemarker
        Configuration configuration = new Configuration(Configuration.getVersion());
        File htmlFile=null;
        //加载模板
        //选指定模板路径,classpath下templates下
        //得到classpath路径
        try {
            String classpath = this.getClass().getResource("/").getPath();
            configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
            //设置字符编码
            configuration.setDefaultEncoding("utf-8");

            //指定模板文件名称
            Template template = configuration.getTemplate("course_template.ftl");

            //准备数据
            CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(courseId);

            Map<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewInfo);

            //静态化
            //参数1：模板，参数2：数据模型
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            //将静态化内容输出到文件中
            InputStream inputStream = IOUtils.toInputStream(html,"utf-8");
            htmlFile = File.createTempFile("course",".html");
            //输出流
            FileOutputStream outputStream = new FileOutputStream(htmlFile);
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            log.debug("生成静态页面出错，课程id：{}",courseId,e);
        }

        return htmlFile;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        try {
            MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
            String course = mediaServiceClient.upload(multipartFile, "course/"+courseId+".html");
            if(course==null){
                log.debug("远程调用走降级逻辑，得到上传的结果为null，课程id：{}",courseId);
                XueChengPlusException.cast("上传静态文件异常");
            }
        } catch (Exception e) {
            XueChengPlusException.cast("上传静态文件异常");
        }
    }

    private void saveCoursePublishMessage(Long courseId){
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if(mqMessage==null){
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
    }

}
