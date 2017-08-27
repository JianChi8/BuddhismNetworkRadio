package com.jianchi.fsp.buddhismnetworkradio;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import com.alibaba.fastjson.JSON;
import com.beardedhen.androidbootstrap.TypefaceProvider;
import com.jianchi.fsp.buddhismnetworkradio.api.ChannelType;
import com.jianchi.fsp.buddhismnetworkradio.db.DBHelper;
import com.jianchi.fsp.buddhismnetworkradio.mp3.FtpServer;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3DownloadThread;
import com.jianchi.fsp.buddhismnetworkradio.tools.FileUtils;
import com.jianchi.fsp.buddhismnetworkradio.tools.MyLog;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

/**
 * Created by fsp on 16-7-13.
 * 保存状态，以便在屏幕旋转后使用
 */
public class BApplication extends Application {

    /*
    节目时间表:
    视频音频播放器
    线路选择:http://www.amtb.tw/tvchannel/play-1-revised.asp
    最新讯息:http://www.amtb.tw/tvchannel/show_marquee.asp
    经文讲义:http://ft.hwadzan.com/mycalendar/mycalendar_embed_livetv.php?calendar_name=livetv
    * */

    public HashMap<ChannelType, String> programsListUrlMap;

    //载入错误次数
    //public int errTimes = 0;

    public boolean beInit;

    private ServiceConnection sc;
    public IMusic music;
    Mp3Receiver mp3Receiver;

    public String selectServerCode="";
    public String  selectCityCode="";

    public Mp3DownloadThread mp3DownloadThread;

    @Override
    public void onCreate() {
        super.onCreate();
        TypefaceProvider.registerDefaultIconSets();

        testAllFtpServer();

        programsListUrlMap = new HashMap<ChannelType, String>();
        List<String> programSchedulesUrl = FileUtils.readRawAllLines(this, R.raw.programs_schedule);
        for(String surl : programSchedulesUrl){
            String[] ss = surl.split(",");
            programsListUrlMap.put(Enum.valueOf(ChannelType.class, ss[0]), ss[1]);
        }

        beInit = false;

        DBHelper.init(this);


        sc = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                boolean isOk =false;
                if(iBinder!=null) {
                    music = (IMusic) iBinder;
                    initButtonReceiver();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };

        Intent intentMp3 = new Intent(this, BMp3Service.class);
        bindService(intentMp3, sc, BIND_AUTO_CREATE);

    }

    /**
     * 检测网络是否可用
     * @return
     */
    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    public FtpServer getFtpServer() {

        return fastFtpServer;
/*
        String json = FileUtils.readRawAllText(this, R.raw.ftpservers);
        List<FtpServer> ftpServers = JSON.parseArray(json, FtpServer.class);
        HashMap<String, FtpServer> ftpServerHashMap = new HashMap<>();
        for (FtpServer ftpServer : ftpServers) {
            ftpServerHashMap.put(ftpServer.name, ftpServer);
        }

  {"name":"ftp1","server":"ftp1.amtb.tw","anonymous":"","pass":"","port":21}, //台湾
  {"name":"ftp2","server":"ftp2.amtb.cn","anonymous":"","pass":"","port":21}, //江苏电信
  {"name":"ftp8","server":"ftp8.amtb.cn","anonymous":"","pass":"","port":21}, //云南电信
  {"name":"ftpa","server":"ftpa.amtb.cn","anonymous":"","pass":"","port":21}, //山西网通
  {"name":"ftpde","server":"ftp.amtb.de","anonymous":"","pass":"","port":21}  //德国

        FtpServer ftpServer = null;
        //ServersList serversList = data.getServers();
        if (selectServerCode.equals("台湾")) {
            ftpServer = ftpServerHashMap.get("ftp1");
        } else if (selectServerCode.equals("德国")) {
            ftpServer = ftpServerHashMap.get("ftpde");
        } else if (selectServerCode.equals("中国")) {
            if (selectCityCode.equals("江苏")) { //-- 江蘇
                ftpServer = ftpServerHashMap.get("ftp2");
            } else if (selectCityCode.equals("云南")) { //-- 江蘇
                ftpServer = ftpServerHashMap.get("ftp8");
            } else if (selectCityCode.equals("山西")) { //-- 江蘇
                ftpServer = ftpServerHashMap.get("ftpa");
            } else {
                java.util.Random r=new java.util.Random();
                int i = r.nextInt(3);
                ftpServer = ftpServers.get(i+1);
            }
        }

        if(ftpServer==null){
            java.util.Random r=new java.util.Random();
            int i = r.nextInt(ftpServers.size()-1);
            ftpServer = ftpServers.get(i);
        }

        return ftpServer;
        */
    }

    public FtpServer fastFtpServer;

    private void testAllFtpServer(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String json = FileUtils.readRawAllText(BApplication.this, R.raw.ftpservers);
                List<FtpServer> ftpServers = JSON.parseArray(json, FtpServer.class);
                long useTime = 6000000;
                for (FtpServer ftpServer : ftpServers) {
                    TestFtpSpeed testFtpSpeed = new TestFtpSpeed(ftpServer);
                    new Thread(testFtpSpeed).start();
                }
            }
        }).start();
    }

    class TestFtpSpeed implements Runnable {
        private static final long maxTime = 600000;
        private FtpServer ftpServer;
        public TestFtpSpeed(FtpServer ftpServer){
            this.ftpServer = ftpServer;
        }

        @Override
        public void run() {
            long t = testFtpServer(ftpServer.server);
            if(fastFtpServer==null && t!=maxTime){
                fastFtpServer=ftpServer;
            }
        }

        private long testFtpServer(String host) {
            long start_time = System.currentTimeMillis();
            long userTime = maxTime;
            int port = FTP.DEFAULT_PORT;
            String user="anonymous",pass="";
            try {
                FTPClient ftpClient = new FTPClient();
                ftpClient.setDataTimeout(8000);
                ftpClient.connect(host, port);
                boolean isConnect = FTPReply.isPositiveCompletion(ftpClient.getReplyCode());
                if(isConnect) {
                    boolean isLogin = ftpClient.login(user, pass);
                    if (isLogin) {
                        ftpClient.logout();
                        userTime = System.currentTimeMillis() - start_time;
                    }
                }
            } catch (IOException e) {

            }

            return userTime;
        }
    }



    public String readRawFile(int rawId)
    {
        String tag = "readRawFile";
        String content=null;
        Resources resources=getResources();
        InputStream is=null;
        try{
            is=resources.openRawResource(rawId);
            byte buffer[]=new byte[is.available()];
            is.read(buffer);
            content=new String(buffer);
            MyLog.i(tag, "read:"+content);
        }
        catch(IOException e)
        {
            MyLog.e(tag, e.getMessage());
        }
        finally
        {
            if(is!=null)
            {
                try{
                    is.close();
                }catch(IOException e)
                {
                    MyLog.e(tag, e.getMessage());
                }
            }
        }
        return content;
    }

    /**
     * 初始化广播，需要在Service或者Activity开始的时候就调用
     */
    public void initButtonReceiver() {
        mp3Receiver = new Mp3Receiver(music);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Mp3Biner.PLAY_PAUSE_BUTTON);
        registerReceiver(mp3Receiver, intentFilter);

        String B_PHONE_STATE = TelephonyManager.ACTION_PHONE_STATE_CHANGED;
        PhoneCallReceiver phoneCallReceiver = new PhoneCallReceiver(music);
        IntentFilter phoneStateIntentFilter = new IntentFilter();
        phoneStateIntentFilter.addAction(B_PHONE_STATE);
        registerReceiver(phoneCallReceiver, phoneStateIntentFilter);
    }
}
