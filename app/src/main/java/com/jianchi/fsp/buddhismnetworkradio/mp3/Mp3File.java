package com.jianchi.fsp.buddhismnetworkradio.mp3;

/**
 * Created by fsp on 17-8-7.
 * 可用json读取
 * ftp://ftp2.amtb.cn	中国江苏常州电信	mp3	无
 ftp://ftp8.amtb.cn	中国云南昆明电信	56k	无
 ftp://ftpa.amtb.cn	中国山西太原联通	mp3	无
 ftp://ftp.amtb.de	德国	mp3	子目录分类  "52-080_128k_未合併"替换"52-080"
 ftp://ftp1.amtb.tw	台湾	mp3	无

 */

public class Mp3File {

    public enum LocaleMp3State{
        Playing,
        Downloaded,
        Downloading,
        NoDownload,
        RemoteFileNoexist
    }

    public String fileName;//":"01-001-0001.mp3",
    public String url;//":"01-001"
    public LocaleMp3State state;

    public String getFtpPath(FtpServer server){
        String mp3dir = "/mp3/";
        if(server.name.equals("ftp8")){
            mp3dir="/56k/";
        }

        if(server.name.equals("ftpde")){
            if(url.equals("52-080")){
                mp3dir += "52-080_128k_未合併/";
            } else {
                mp3dir += url + "/";
            }
        }

        if(fileName.startsWith("12-17-")){
            mp3dir+= "12-017"+fileName.substring(5);
        } else {
            mp3dir += fileName;
        }

        return mp3dir;
    }
}
