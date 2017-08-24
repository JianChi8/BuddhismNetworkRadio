package com.jianchi.fsp.buddhismnetworkradio.mp3;

/**
 * Created by fsp on 17-8-20.
 */

    //枚举类DownloadStatus代码
    public enum DownloadStatus {
        Remote_File_Noexist, //远程文件不存在
        Local_Bigger_Remote, //本地文件大于远程文件
        Download_New_Success,    //全新下载文件成功
        NoNextDownLoadMp3,
        ReSetMp3State,
        NoWifiNet,
        WifiNetOk,
        CONNECT_FAIL
    }