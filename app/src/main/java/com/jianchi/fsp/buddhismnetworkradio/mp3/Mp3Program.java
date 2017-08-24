package com.jianchi.fsp.buddhismnetworkradio.mp3;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.jianchi.fsp.buddhismnetworkradio.tools.FileUtils;
import com.jianchi.fsp.buddhismnetworkradio.R;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by fsp on 17-8-7.
 * 显示于外部的MP3
 * 保存 名称，id，信息，下载量
 * 进度 文件名，位置
 * 最好存入sqlite中
 */

public class Mp3Program {

    public Mp3Program(){

    }

    public int dbRecId = -1;

    /*
    名称
     */
    public String name;

    /*
    节目编号，并非MP3文件编号，用来获取播放文件
     */
    public String id;

    /*
    其它信息
     */
    public String info;

    /*
    热度
     */
    public int num;

    /*
    当前播放文件
     */
    public String curPlayFile="";

    /*
    播放时间
     */
    public int postion=-1;

    public String[] queryMp3Files(Context context) {

        List<String> ss = FileUtils.readRawAllLines(context, R.raw.mp3files);
        String fsStr = "";
        String id2 = id+":";
        for(String s : ss){
            if(s.startsWith(id2)) {
                fsStr = s;
                break;
            }
        }

        if(fsStr.isEmpty()){
            return null;
        }

        fsStr = fsStr.substring(id2.length(), fsStr.length()-1);

        return fsStr.split(",");
    }
}
