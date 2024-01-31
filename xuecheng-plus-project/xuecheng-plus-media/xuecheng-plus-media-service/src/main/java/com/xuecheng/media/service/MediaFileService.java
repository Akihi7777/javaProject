package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;

import java.io.File;

/**
 * @description 媒资文件管理业务类
 * @author Mr.M
 * @date 2022/9/10 8:55
 * @version 1.0
 */
public interface MediaFileService {

   public PageResult<MediaFiles> queryMediaFiles(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

   public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath,String objectName);

   public RestResponse<Boolean> checkFile(String fileMd5);

   public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex);

   public RestResponse uploadChunk(String fileMd5,int chunk,String localFilePath);

   public RestResponse mergechunks(Long companyId,String fileMd5,int chunkTotal,UploadFileParamsDto uploadFileParamsDto);

   public File downloadFileFromMinIO(String bucket,String mergeFilePath);

   public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,String bucket, String objectName);

   public String getMimeType(String extension);

   public String getFilePathByMd5(String fileMd5, String extName);

   public MediaFiles getFileById(String mediaId);
}
