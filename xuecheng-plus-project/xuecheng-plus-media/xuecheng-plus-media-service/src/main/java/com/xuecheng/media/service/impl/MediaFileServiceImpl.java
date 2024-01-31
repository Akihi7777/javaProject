package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.lang.management.MonitorInfo;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MinioClient minioClient;

    @Autowired
    MediaFileServiceImpl currentProxy;

    @Autowired
    MediaProcessMapper mediaProcessMapper;

    @Value("${minio.bucket.files}")
    private String bucket_files;

    @Value("${minio.bucket.videofiles}")
    private String videos_file;


    @Override
    public PageResult<MediaFiles> queryMediaFiles(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath,String objectName) {
        File file = new File(localFilePath);
        if(!file.exists()){
            XueChengPlusException.cast("文件不存在！");
        }
        String fileName = uploadFileParamsDto.getFilename();
        //获取后缀
        String suffix=fileName.substring(fileName.lastIndexOf('.'));
        String mimeType = getMimeType(suffix);
        //构造objectName
        LocalDate currentDay = LocalDate.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String formatDay = currentDay.format(dateTimeFormatter);
        String md5 = getMD5(file);
        if(md5==null){
            XueChengPlusException.cast("上传文件失败！");
        }
        if(StringUtils.isEmpty(objectName)){
            objectName=formatDay+"/"+md5+suffix;
        }
        //上传文件
        addMediaFilesToMinIO(localFilePath,mimeType,bucket_files,objectName);
        //将信息保存到数据库
        MediaFiles mediaFiles = currentProxy.saveToDatabase(companyId,uploadFileParamsDto, objectName, md5,bucket_files);
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);
        return uploadFileResultDto;
    }

    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        //先查数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if(mediaFiles!=null) {
            String bucket = mediaFiles.getBucket();
            String filePath = mediaFiles.getFilePath();
            InputStream inputStream=null;
            try {
                inputStream=minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(filePath).build());
                if(inputStream!=null){
                    //文件已存在
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("获取minio文件失败");
            }
        }
        //文件不存在
        return RestResponse.success(false);
    }

    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
        String chunkPath= getChunkPath(fileMd5, chunkIndex);
        InputStream inputStream=null;
        try {
            inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(videos_file).object(chunkPath).build());
            if(inputStream!=null){
                return RestResponse.success(true);
            }
        } catch (Exception e) {
        }
        return RestResponse.success(false);
    }

    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String localFilePath) {
        String chunkPath= getChunkPath(fileMd5, chunk);
        String mimeType = getMimeType("");
        boolean b = addMediaFilesToMinIO(localFilePath, mimeType, videos_file, chunkPath);
        if(b){
            return RestResponse.success(true);
        }
        return RestResponse.validfail(false,"上传文件失败");
    }

    @Override
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        //合并分块
        //获取后缀名
        String filename = uploadFileParamsDto.getFilename();
        String extName = filename.substring(filename.lastIndexOf('.'));
        String mergeFilePath = getFilePathByMd5(fileMd5, extName);
        //获取块来源
        List<ComposeSource> sourcesList= Stream.iterate(0,i -> ++i).limit(chunkTotal).map(i->ComposeSource.builder().bucket(videos_file).object(getChunkPath(fileMd5,i)).build()).collect(Collectors.toList());
        try {
            minioClient.composeObject(ComposeObjectArgs.builder().bucket(videos_file).object(mergeFilePath).sources(sourcesList).build());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并文件失败，bucket:{},fileMD5:{}",videos_file,fileMd5);
        }
        //检查合并后的文件md5值与本地是否一致
        File minioFile = downloadFileFromMinIO(videos_file,mergeFilePath);
        if(minioFile!=null){
            try {
                InputStream newFileInputStream = new FileInputStream(minioFile);
                String s = DigestUtils.md5Hex(newFileInputStream);
                if(!s.equals(fileMd5)){
                    return RestResponse.validfail(false, "文件合并校验失败，最终上传失败");
                }
            } catch (Exception e) {
                log.debug("校验文件失败,fileMd5:{},异常:{}",fileMd5,e.getMessage(),e);
            }
        }
        //存入数据库
        currentProxy.saveToDatabase(companyId,uploadFileParamsDto,mergeFilePath,fileMd5,videos_file);
        //清除分块
        clearChunkFiles(getChunkPath(fileMd5),chunkTotal);
        return RestResponse.success(true);
    }

    private void clearChunkFiles(String chunkPath,int chunkTotal){
        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i).limit(chunkTotal).map(i -> new DeleteObject(chunkPath.concat(Integer.toString(i)))).collect(Collectors.toList());
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(RemoveObjectsArgs.builder().bucket(videos_file).objects(deleteObjects).build());
            results.forEach(r -> {
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清楚分块文件失败,objectname:{}", deleteError.objectName(), e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清除分块文件失败,chunkFileFolderPath:{}", chunkPath, e);
        }

}

    public File downloadFileFromMinIO(String bucket,String mergeFilePath){
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(mergeFilePath).build());
            File tempFile = File.createTempFile("minio", ".merge");
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            IOUtils.copy(stream,fileOutputStream);
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("下载合并视频失败，bucket:{},mergeFilePath:{}",bucket,mergeFilePath);
        }
        return null;
    }

    public String getFilePathByMd5(String fileMd5, String extName){
        return fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/"+fileMd5+extName;
    }

    @Override
    public MediaFiles getFileById(String mediaId) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
        return mediaFiles;
    }

    private String getChunkPath(String fileMd5,int chunkIndex){
        String chunkDic = fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5+"/"+"chunk"+"/"+chunkIndex;
        return chunkDic;
    }

    private String getChunkPath(String fileMd5){
        String chunkDic = fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5+"/"+"chunk"+"/";
        return chunkDic;
    }

    @Transactional
    public MediaFiles saveToDatabase(Long companyId, UploadFileParamsDto uploadFileParamsDto, String objectName, String md5,String bucket){
        MediaFiles mediaFiles = mediaFilesMapper.selectById(md5);
        if(mediaFiles==null) {
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            mediaFiles.setId(md5);
            mediaFiles.setCompanyId(companyId);
            mediaFiles.setBucket(bucket);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setFileId(md5);
            mediaFiles.setUrl("/"+bucket+"/"+objectName);
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setAuditStatus("002003");
            mediaFiles.setStatus("1");
            int insert = mediaFilesMapper.insert(mediaFiles);
            if(insert<=0){
                XueChengPlusException.cast("文件保存到本地失败！");
                log.error("文件保存到数据库失败：{}",mediaFiles.toString());
            }
            //记录待处理任务
            addWaitingTask(mediaFiles);
        }

        return mediaFiles;
    }

    public void addWaitingTask(MediaFiles mediaFiles){
        String filename = mediaFiles.getFilename();
        String suffix = filename.substring(filename.lastIndexOf("."));
        String mimeType = getMimeType(suffix);
        if(mimeType.equals("video/x-msvideo")){
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles,mediaProcess);
            mediaProcess.setStatus("1");
            mediaProcess.setCreateDate(LocalDateTime.now());
            mediaProcess.setUrl(null);
            int insert = mediaProcessMapper.insert(mediaProcess);
            if(insert<=0){
                XueChengPlusException.cast("上传视频失败");
            }
        }
    }

    private String getMD5(File file){
        try (InputStream inputStream = new FileInputStream(file)){
            String md5Hex = DigestUtils.md5Hex(inputStream);
            return md5Hex;
        }
        catch (Exception e){
            log.error("读取本地文件出错！");
            e.printStackTrace();
            return null;
        }
    }

    //根据扩展名取出mimeType
    public String getMimeType(String extension) {
        if (extension==null) {
            return "";
        }
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
        if(extensionMatch!=null){
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,String bucket, String objectName){
        try {
            //上传文件的参数信息
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucket)//桶
                    .filename(localFilePath) //指定本地文件路径
                    .object(objectName)//对象名 放在子目录下
                    .contentType(mimeType)//设置媒体文件类型
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
            return true;
        }
        catch (Exception e){
            e.printStackTrace();
            log.error("文件上传出错，bucket:{},objectName:{}",bucket,objectName);
        }
        return false;
    }


}
