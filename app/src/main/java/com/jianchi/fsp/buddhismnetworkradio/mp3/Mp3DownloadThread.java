package com.jianchi.fsp.buddhismnetworkradio.mp3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by fsp on 17-8-18.
 * 启动下载线程
 * 若有下载任务则开始下载
 * 超出当前之后5个文件，则停止
 * 否则检测下载的文件数量，如果少于10个，则删除最旧的，开始下载任务
 *
 * 当前播放的文件
 * 已经缓存的文件列表
 * 当前文件之后的文件数量
 *
 * 保存下载进度，下载进度文件，存储内容：下载的program，当前文件名，最后保存的位置。
 *
 * 有新文件下载完成，则加入文件列表
 *
 * 事件通知下载进度
 */

public class Mp3DownloadThread extends BroadcastReceiver implements Runnable{

    public boolean onlyWifi;
    public int maxDownloadFiles;
    Thread thread;
    Context context;

    public String mp3ProgramId;

    public List<Mp3File> mp3s;
    public int curPlayMp3sIdx;
    Mp3File curDownMp3 = null;

    private boolean isRun;

    FtpServer ftpServer;
    public FTPClient ftpClient = new FTPClient();
    public File localMp3Dir;

    boolean testNetOk = false;
    boolean programChange = false;

    Object synchronizedObj;
    Object synchronizedNetObj;
    DownloadEvenListener downloadEvenListener;

    public Mp3DownloadThread(Context context, List<Mp3File> mp3s, FtpServer ftpServer, int maxDownloadFiles, boolean onlyWifi, DownloadEvenListener downloadEvenListener, String mp3ProgramId){
        thread = new Thread(this);
        this.mp3s = mp3s;
        this.ftpServer = ftpServer;
        this.context = context;
        this.maxDownloadFiles = maxDownloadFiles;
        this.onlyWifi = onlyWifi;
        this.downloadEvenListener = downloadEvenListener;
        this.mp3ProgramId = mp3ProgramId;

        //注册监听网络状态变更
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(this, mFilter);

        localMp3Dir = new File(context.getExternalCacheDir(),"localmp3");
        if(!localMp3Dir.exists())
            localMp3Dir.mkdirs();

        synchronizedObj = new Object();
        synchronizedNetObj = new Object();

        curPlayMp3sIdx =-1;
        isRun = false;

        ftpClient = new FTPClient();

        //resetMp3State(getMp3Files());
    }

    public void setDownloadEvenListener(DownloadEvenListener downloadEvenListener){
        this.downloadEvenListener = downloadEvenListener;
    }

    public void setDownloadMp3(List<Mp3File> mp3s, String mp3ProgramId, int curPlayMp3sIdx){
        this.mp3s = mp3s;
        this.mp3ProgramId = mp3ProgramId;
        programChange=true;
        start(curPlayMp3sIdx);
    }

    private void resetMp3State(List<File> mp3Files) {

        Set<String> downloadedFiles = new HashSet<>();
        Set<String> cacheFiles = new HashSet<>();
        for (File f : mp3Files) {
            String name = f.getName();
            if (name.endsWith(".cache"))
                cacheFiles.add(name.substring(0, name.length() - 6));
            else
                downloadedFiles.add(f.getName());
        }

        for (int i = 0; i < mp3s.size(); i++) {
            Mp3File mf = mp3s.get(i);

            //不存在的文件则保持这个状态，并在获取下载文件时跳过它
            if (mf.state == Mp3File.LocaleMp3State.RemoteFileNoexist)
                continue;

            if (mf.state == Mp3File.LocaleMp3State.Playing)
                continue;

            if (downloadedFiles.contains(mf.fileName)) {
                mf.state = Mp3File.LocaleMp3State.Downloaded;
            } else if (cacheFiles.contains(mf.fileName)) {
                mf.state = Mp3File.LocaleMp3State.Downloading;
            } else {
                mf.state = Mp3File.LocaleMp3State.NoDownload;
            }
        }
    }

    /**
     *
     * 1. wifiManager.WIFI_STATE_DISABLED (1)
     　2. wifiManager..WIFI_STATE_ENABLED (3)
     　3. wifiManager..WIFI_STATE_DISABLING (0)
     　4 wifiManager..WIFI_STATE_ENABLING  (2)
     * @return
     */
    private boolean isWifiOk(){
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if(wifiManager != null){
            return(wifiManager.getWifiState()==WifiManager.WIFI_STATE_ENABLED);
        }
        return false;
    }

    private boolean isNetworkOnline() {
        Runtime runtime = Runtime.getRuntime();
        synchronized (this) {
            try {
                Process ipProcess = runtime.exec("ping -c 1 114.114.114.114");
                int exitValue = ipProcess.waitFor();
                return (exitValue == 0);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean isNetOk(){
        if(isWifiOk())
            if(isNetworkOnline())
                return true;
        return false;
    }

    /** *//**
     * 连接到FTP服务器
     * @return 是否连接成功
     * @throws IOException
     */
    private boolean connect(){

        try {
            ftpClient.connect(ftpServer.server, 21);
            String user="anonymous",pass="";
            if(FTPReply.isPositiveCompletion(ftpClient.getReplyCode())){
                if(ftpClient.login(user, pass)){
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        disconnect();
        return false;
    }

    /** *//**
     * 断开与远程服务器的连接
     * @throws IOException
     */
    private void disconnect(){
        if(ftpClient.isConnected()){
            try {
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 网络状态变更，下载线程的网络状态变更放到这里
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        /**
         * 从 GPRS 到 WIFI，程序至少会收到3个Broadcast
         第一个是连接到WIFI
         第二个是断开GPRS
         第三个是连接到WIFI

         从WIFI到GPRS，程序至少会收到2个Broadcast
         第一个是断开Wifi
         第二个是连接到GPRS
         */

        //网络状态由可用改为了不可用，不用管它，线程中会自动跳出错误的
        //网络状态由不可用改为了可用，这里要发送消息，让程序可用
        if(!testNetOk){
            testNetOk = isNetOk();
            if(testNetOk){
                synchronized (synchronizedNetObj) {
                    //发送通知，WIFI网络连接成功，可能有问题，好象必须在创建的线程中调用
                    downloadEvenListener.handleEvent(DownloadStatus.WifiNetOk);
                    synchronizedNetObj.notify();
                }
            }
        }
    }


    public void start(int curPlayMp3sIdx){
        this.curPlayMp3sIdx = curPlayMp3sIdx;

        curDownMp3 = nextDownloadMp3();

        isRun = true;

        //判断是否网络状态可用，如果可用就开启线程

        if(!thread.isAlive())
            thread.start();
        else {
            synchronized (synchronizedObj) {
                synchronizedObj.notify();
            }
        }
    }

    public void stop(){
        isRun = false;
        synchronized (synchronizedObj) {
            synchronizedObj.notify();
        }
    }

    List<File> getMp3Files(){
        File[] fs =localMp3Dir.listFiles();
        List<File> mp3Files = new ArrayList<>();
        for(int i=0; i<fs.length; i++) {
            //判断是否是在要下载的文件列表中
            mp3Files.add(fs[i]);
        }

        Collections.sort(mp3Files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                long x = o1.lastModified();
                long y = o2.lastModified();
                return (x < y) ? -1 : ((x == y) ? 0 : 1);
            }
        });

        return mp3Files;
    }

    private List<Mp3File> getNeedDownloadMp3s(){
        int b = maxDownloadFiles/2;
        int b2 = maxDownloadFiles-b;

        int startIdx = curPlayMp3sIdx-b;
        if(startIdx<0)
            startIdx=0;

        int endIdx = curPlayMp3sIdx+b2;
        if(endIdx>mp3s.size())
            endIdx=mp3s.size();

        List<Mp3File> dmp3 = new ArrayList<>();
        for(int i=startIdx; i<endIdx; i++){
            dmp3.add(mp3s.get(i));
        }
        return dmp3;
    }

    Mp3File nextDownloadMp3(){
        //调度规则
        //返回 null 表示暂时不进行下载。仅当满足：当前播放文件前后共maxDownloadFiles下载完成，或已经下载到最后一个文件时返回
        //检测是否达到了maxDownloadFiles，没达到则下载下一个文件，否则删除不在以当前播放文件为中心的最多maxDownloadFiles之外的，最旧文件

        //RemoteFileNoexist时，跳过此文件

        List<File> mp3Files = getMp3Files();
        List<Mp3File> neddMP3 = getNeedDownloadMp3s();
        boolean found;
        //超过最大下载量，并且最后一个目标文件没有正在下载中，则删除
        if (mp3Files.size()>=maxDownloadFiles){
            //删除一个非目标文件，并且是最老的文件
            for(int i = mp3Files.size()-1; i>=0; i--){
                found = false;
                for(Mp3File mp3:neddMP3){
                    if(mp3Files.get(i).getName().startsWith(mp3.fileName)){
                        found = true;
                        break;
                    }
                }
                if(!found){
                    mp3Files.get(i).delete();
                    mp3Files.remove(i);
                    if(mp3Files.size()<maxDownloadFiles)
                        break;
                }
            }
        }

        resetMp3State(mp3Files);

        //获取下一个mp3文件名
        //curPlayMp3sIdx
        int b = maxDownloadFiles/2;
        int b2 = maxDownloadFiles-b;

        int startIdx = curPlayMp3sIdx-b;
        if(startIdx<0)
            startIdx=0;

        int endIdx = curPlayMp3sIdx+b2;
        if(endIdx>mp3s.size())
            endIdx=mp3s.size();

        for(int i = curPlayMp3sIdx; i<endIdx; i++){

            Mp3File mp3 = mp3s.get(i);

            //跳过已确定不存在的文件
            if(mp3.state== Mp3File.LocaleMp3State.RemoteFileNoexist) {
                if(endIdx>mp3s.size())
                    endIdx++;
                continue;
            }

            //判断这个mp3文件是否存在，如果存在则进行下一个，若不存在，则看是否存在cache文件，有则返回cache
            File mp3File = new File(localMp3Dir, mp3.fileName);
            if(!mp3File.exists()){
                mp3.state = Mp3File.LocaleMp3State.Downloading;

                downloadEvenListener.handleEvent(DownloadStatus.ReSetMp3State);
                return mp3;
            }
        }

        downloadEvenListener.handleEvent(DownloadStatus.ReSetMp3State);
        return null;
    }

    @Override
    public void run() {
        //下载完成后，一直处于待命状态，在程序关闭后，关闭线程
        while (isRun){
            programChange=false;

            curDownMp3 = nextDownloadMp3();

            if(curDownMp3==null){
                try {
                    synchronized (synchronizedObj) {
                        //发送状态通知
                        downloadEvenListener.handleEvent(DownloadStatus.NoNextDownLoadMp3);
                        //等待开始播放或播放下一个
                        synchronizedObj.wait();
                        //从新获取
                        continue;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if(!isRun) return;

            testNetOk = isNetOk();
            //如果网络状态不可用，就退出，但isRun还为true
            if(!testNetOk){
                try {
                    synchronized (synchronizedNetObj) {
                        //发送通知，没有WIFI网络
                        downloadEvenListener.handleEvent(DownloadStatus.NoWifiNet);
                        //等待开始播放或播放下一个
                        synchronizedNetObj.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            if(!isRun) return;

            curDownMp3.state = Mp3File.LocaleMp3State.Downloading;

            //下载文件
            download(curDownMp3);
        }
    }


    /** *//**
     * 从FTP服务器上下载文件,支持断点续传，上传百分比汇报
     * @param mp3 远程文件路径 @param local 本地文件路径
     * @return 上传的状态
     * @throws IOException
     */
    public void download(Mp3File mp3) {
        if (connect()) {

            String remote = getFtpPath(ftpServer, mp3.fileName) + mp3.fileName;
            File f = new File(localMp3Dir, mp3.fileName + ".cache");
            long lRemoteSize = 0;
            long localSize = 0;
            try {

                //设置被动模式
                ftpClient.enterLocalPassiveMode();
                //设置以二进制方式传输
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                //检查远程文件是否存在
                FTPFile[] files = ftpClient.listFiles(remote);
                if (files.length != 1) {
                    System.out.println("远程文件不存在");
                    mp3.state = Mp3File.LocaleMp3State.RemoteFileNoexist;
                    downloadEvenListener.handleEvent(DownloadStatus.Remote_File_Noexist);
                    //这个时候，这个文件将不再会被加到下载列表，除非下次再下
                    disconnect();
                    return;
                }
                lRemoteSize = files[0].getSize();
                localSize = 0;
                //本地存在文件，进行断点下载
                if (f.exists()) {
                    localSize = f.length();
                    //判断本地文件大小是否大于远程文件大小
                    if (localSize >= lRemoteSize) {
                        System.out.println("本地文件大于远程文件，下载中止");
                        f.renameTo(new File(localMp3Dir, mp3.fileName));
                        mp3.state = Mp3File.LocaleMp3State.Downloaded;
                        downloadEvenListener.handleEvent(DownloadStatus.Local_Bigger_Remote);
                        return;
                    }

                    ftpClient.setRestartOffset(localSize);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            OutputStream out = null;
            InputStream in = null;
            try {
                out = new FileOutputStream(f, true);
                in = ftpClient.retrieveFileStream(remote);
                byte[] bytes = new byte[1024];
                long step = lRemoteSize / 100;
                long process = localSize / step;
                int c;
                while ((c = in.read(bytes)) != -1) {
                    out.write(bytes, 0, c);
                    localSize += c;
                    long nowProcess = localSize / step;
                    if (nowProcess > process) {
                        process = nowProcess;
                        downloadEvenListener.updateProcess(process, mp3.fileName);
                        //if (process % 10 == 0) {
                            //System.out.println("下载进度：" + process);
                        //}
                    }
                    if (!isRun) {
                        break;
                    }

                    if(curDownMp3!=mp3)
                        break;

                    if(programChange)
                        break;

                }
                boolean upNewStatus = localSize >= lRemoteSize;
                if (upNewStatus) {
                    mp3.state = Mp3File.LocaleMp3State.Downloaded;
                    f.renameTo(new File(localMp3Dir, mp3.fileName));
                    downloadEvenListener.handleEvent(DownloadStatus.Download_New_Success);
                }
            } catch (IOException e) {
                //肯定是网络出错了吧
                e.printStackTrace();
            } finally {
                try {
                    if (out != null)
                        out.close();
                    if (in != null)
                        in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                disconnect();
            }
            return;
        } else {
            isRun = false;
            downloadEvenListener.handleEvent(DownloadStatus.CONNECT_FAIL);
        }
    }

    public static String getFtpPath(FtpServer server, String url){
        String mp3dir = "/mp3/";
        if(server.name.equals("ftp8")){
            mp3dir="/56k/";
        }

        mp3dir+=url.substring(0,2)+"/";

        if(server.name.equals("ftpde")){
            if(url.equals("52-080")){
                mp3dir += "52-080_128k_未合併/";
            } else {
                mp3dir += url + "/";
            }
        }

        return mp3dir;
    }
}
