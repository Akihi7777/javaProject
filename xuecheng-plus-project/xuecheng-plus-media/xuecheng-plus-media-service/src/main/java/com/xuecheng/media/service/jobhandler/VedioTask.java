package com.xuecheng.media.service.jobhandler;

import com.xuecheng.media.service.MediaFileProcessService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class VedioTask {
    @Autowired
    MediaFileProcessService mediaFileProcessService;

    private static Logger logger = LoggerFactory.getLogger(SampleXxlJob.class);

    @XxlJob("vedioJobHandler")
    public void vedioJobHandler() throws Exception {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
    }
}
