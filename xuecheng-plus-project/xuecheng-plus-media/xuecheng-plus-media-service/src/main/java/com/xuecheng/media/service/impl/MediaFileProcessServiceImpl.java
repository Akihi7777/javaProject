package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Service
public class MediaFileProcessServiceImpl implements MediaFileProcessService {
    @Autowired
    MediaProcessMapper mediaProcessMapper;

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MediaFileService mediaFileService;

    @Autowired
    MediaProcessHistoryMapper mediaProcessHistoryMapper;

    @Value("${minio.bucket.videofiles}")
    private String videos_file;

    @Value("${videoprocess.ffmpegpath}")
    String ffmpegpath;


    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        List<MediaProcess> mediaProcesses = mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, count);
        return mediaProcesses;
    }

    @Override
    public boolean startTask(long id) {
        int i = mediaProcessMapper.startTask(id);
        return i<=0?false:true;
    }

    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {
        int shardIndex = XxlJobHelper.getShardIndex(); //当前执行器的序列号
        int shardTotal = XxlJobHelper.getShardTotal();
        int processors = Runtime.getRuntime().availableProcessors();
        List<MediaProcess> mediaProcesses = mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, processors);
        int size = mediaProcesses.size();
        if(size<=0){
            log.debug("取出视频小于等于0条");
            return;
        }
        //启动size个线程的线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(size);
        //计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        //将处理任务加入线程池
        mediaProcesses.forEach(mediaProcess -> {
            threadPool.execute(()->{
                try {
                    Long taskId = mediaProcess.getId();
                    int i = mediaProcessMapper.startTask(taskId);
                    if(i<=0){
                        return;
                    }
                    log.debug("开始执行任务:{}",taskId);
                    String filePath = mediaProcess.getFilePath();
                    String fileId = mediaProcess.getFileId();
                    String bucket = mediaProcess.getBucket();
                    //下载待处理文件到服务器（本地模拟服务器）
                    File originFile = mediaFileService.downloadFileFromMinIO(bucket, filePath);
                    if(originFile==null){
                        log.debug("下载视频到本地出错，桶：{}，路径：{}",bucket,filePath);
                        saveProcessFinishStatus(taskId,"3",fileId,null,"下载视频到本地出错");
                        return;
                    }
                    File tempFile=null;
                    try {
                        tempFile= File.createTempFile("mp4", ".mp4");
                    } catch (IOException e) {
                        log.error("创建临时文件出错");
                        saveProcessFinishStatus(taskId,"3",fileId,null,"创建临时文件出错");
                        return;
                    }
                    String result="";
                    try {
                        String absolutePath = originFile.getAbsolutePath();
                        Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpegpath, absolutePath, tempFile.getName(), tempFile.getAbsolutePath());
                        result= mp4VideoUtil.generateMp4();
                    } catch (Exception e) {
                        log.error("转码出错，出错信息：{}",e.getMessage());
                        saveProcessFinishStatus(taskId,"3",fileId,null,"转码出错");
                        return;
                    }
                    if(!result.equals("success")){
                        log.error("处理视频失败,视频地址:{},错误信息:{}", bucket + filePath, result);
                        saveProcessFinishStatus(taskId,"3",fileId,null,"处理视频失败");
                        return;
                    }
                    //上传到minio
                    String absolutePath = tempFile.getAbsolutePath();
                    String fileName = tempFile.getName();
                    String suffix = fileName.substring(fileName.lastIndexOf("."));
                    String mimeType = mediaFileService.getMimeType(suffix);
                    String objectName = mediaFileService.getFilePathByMd5(fileId, ".mp4");
                    boolean b = mediaFileService.addMediaFilesToMinIO(absolutePath, mimeType, bucket, objectName);
                    if(!b){
                        log.error("上传到minio失败");
                        saveProcessFinishStatus(taskId,"3",fileId,null,"上传到minio失败");
                        return;
                    }
                    String url = "/" + bucket + "/" + objectName;
                    saveProcessFinishStatus(taskId,"2",fileId,url,null);
                } finally {
                    countDownLatch.countDown();
                }
            });
        });
        countDownLatch.await(30, TimeUnit.MINUTES);
    }


    @Override
    @Transactional
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        //任务处理失败
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if(mediaProcess==null){
            return;
        }
        LambdaQueryWrapper<MediaProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MediaProcess::getId,taskId);
        if(status.equals("3")) {
            MediaProcess mediaProcess_u = new MediaProcess();
            mediaProcess_u.setStatus("3");
            mediaProcess_u.setErrormsg(errorMsg);
            mediaProcess_u.setFailCount(mediaProcess.getFailCount()+1);
            mediaProcessMapper.update(mediaProcess_u,queryWrapper);
            log.debug("更新任务处理状态为失败，任务信息:{}",mediaProcess_u);
            return;
        }
        //任务处理成功
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        if(mediaFiles!=null){
            mediaFiles.setUrl(url);
            mediaFilesMapper.updateById(mediaFiles);
        }
        mediaProcess.setStatus("2");
        mediaProcess.setUrl(url);
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcessMapper.updateById(mediaProcess);
        //添加进历史任务
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess,mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);
        mediaProcessMapper.deleteById(taskId);
    }
}
