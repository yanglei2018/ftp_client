package com.weather.ftp.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 实现定时从FTP服务器上下载文件
 */
@Component
public class DownLoadFile {
    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Scheduled(initialDelay = 10000, fixedDelay = 3600000)//设置初始时间，间隔时间
    public void timerInit(){
        logger.debug("FTP下载定时任务正在执行");
        System.out.println("执行时间:" + dateFormat.format(new Date()));
        try {
            FTPTransfer.down();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
