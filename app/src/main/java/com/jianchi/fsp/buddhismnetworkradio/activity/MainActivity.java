package com.jianchi.fsp.buddhismnetworkradio.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.alibaba.fastjson.JSON;
import com.jianchi.fsp.buddhismnetworkradio.BApplication;
import com.jianchi.fsp.buddhismnetworkradio.R;
import com.jianchi.fsp.buddhismnetworkradio.api.Channel;
import com.jianchi.fsp.buddhismnetworkradio.api.ChannelType;
import com.jianchi.fsp.buddhismnetworkradio.api.Server;
import com.jianchi.fsp.buddhismnetworkradio.api.ServersList;
import com.jianchi.fsp.buddhismnetworkradio.tools.MyLog;
import com.jianchi.fsp.buddhismnetworkradio.tools.TW2CN;
import com.jianchi.fsp.buddhismnetworkradio.video.VideoMenuManager;

public class MainActivity extends AppCompatActivity {

    //region 变量区
    /**
     * 视频播放器
     */
    VideoView videoView;

    /**
     * 仅声音选择按扭
     */
    CheckBox cb_onlySound;

    /**
     * 播放按扭
     */
    private ImageButton bt_play;

    /**
     * 播放按扭
     */
    private ImageButton bt_select_server;

    /**
     * 播放器外框
     */
    private FrameLayout player_frame;

    /**
     * 加载视频动画
     */
    ProgressBar proBar;
    int proBarThreadId = 0;

    /**
     * 管理播放器周边按扭的类
     */
    VideoMenuManager menuManager;

    /**
     * 自定义APP类
     */
    BApplication app;

    ListView lv_servers;

    Channel channel;

    ServersList serversList;
    String[] servers;
    int yourChoice;
    int newChoice = -1;

    int errTimes = 0;


    /**
     * 是否为仅声音
     */
    boolean isOnlySound = false;

    /**
     * 记录是否正在播放，以便在恢复时使用
     */
    boolean isPlayingResume = false;

    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.KITKAT) {
            //透明状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //透明导航栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        //获取自定义APP，APP内存在着数据，若为旋转屏幕，此处记录以前的内容
        app = (BApplication)getApplication();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(params);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        channel = new Channel();
        channel.title = intent.getStringExtra("title");
        channel.audioUrl = intent.getStringExtra("audioUrl");
        channel.tvUrl = intent.getStringExtra("tvUrl");

        serversList = new ServersList();
        String json = app.readRawFile(R.raw.servers);
        serversList.servers = JSON.parseArray(json,Server.class);
        serversList.selectServerCode = app.selectServerCode;
        serversList.selectCityCode = app.selectCityCode;

        servers = new String[serversList.servers.size()];
        Server selectedServer = serversList.getSelectedServer();
        for(int i=0; i<serversList.servers.size(); i++) {
            Server s = serversList.servers.get(i);
            servers[i] = TW2CN.getInstance(this).toLocalString(s.title);
            if(s==selectedServer)
                yourChoice = i;
        }

        //判断是否连接到网络
        if(!app.isNetworkConnected()){
            networkFailClose();
        }else {
            proBar = (ProgressBar) findViewById(R.id.progressBar);


            /**
             * 核心函数，用来初始化视频播放器。主要功能有
             * 1、在全屏时进行特别设置
             * 2、处理错误数据
             * 3、处理点击
             */
            //初始化videoView，设置video大小，以及错误处理，以及角屏事件

            //初始化三个关键变量
            player_frame = (FrameLayout) findViewById(R.id.player_frame);

            videoView = (VideoView) findViewById(R.id.videoView);
            //screenLandscape();

            videoView.setVisibility(View.VISIBLE);
            videoView.setBackgroundResource(R.drawable.zcgt);

            videoView.setOnPreparedListener(videoViewOnPreparedListener);

            videoView.setOnErrorListener(videoViewOnErrorListener);

            //LinearLayout videoView_top = (LinearLayout)findViewById(R.id.videoView_top);
            RelativeLayout videoView_bottom = (RelativeLayout)findViewById(R.id.videoView_bottom);

            menuManager=new VideoMenuManager(MainActivity.this, videoView_bottom);//, videoView_top

            videoView.setOnTouchListener(videoViewOnTouchListener);

            //按放按扭
            bt_play = (ImageButton) findViewById(R.id.bt_play);
            bt_play.setOnClickListener(bt_playOnClickListener);

            bt_select_server = (ImageButton) findViewById(R.id.bt_select_server);
            bt_select_server.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showServerListDialog();
                }
            });


            //仅声音按扭
            if(channel.getChannelType() == ChannelType.淨空老法師直播) {
                cb_onlySound = (CheckBox) findViewById(R.id.cb_onlySound);
                cb_onlySound.setOnCheckedChangeListener(cb_onlySoundOnCheckedChangeListener);
            } else {
                cb_onlySound = (CheckBox) findViewById(R.id.cb_onlySound);
                cb_onlySound.setVisibility(View.GONE);
            }

            resetVideoView(true);
        }
    }

    void showServerListDialog(){

        AlertDialog.Builder singleChoiceDialog =
                new AlertDialog.Builder(MainActivity.this);
        singleChoiceDialog.setTitle(R.string.action_select_server);
        // 第二个参数是默认选项，此处设置为0
        singleChoiceDialog.setSingleChoiceItems(servers, yourChoice,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        newChoice = which;
                    }
                });
        singleChoiceDialog.setPositiveButton(R.string.dialog_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (newChoice!=yourChoice) {
                            yourChoice = newChoice;
                            serversList.setSelectedServer(serversList.servers.get(yourChoice));
                            resetVideoView(true);
                        }
                    }
                });
        singleChoiceDialog.setNegativeButton(R.string.dialog_cancle,null);

        singleChoiceDialog.show();
        /**
        lv_servers = (ListView) findViewById(R.id.lv_servers);

         * 线路选择事件
        lv_servers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Server si = (Server) view.getTag();

                if (!si.title.equals(serversList.getSelectedServer().title)) {
                    serversList.setSelectedServer(si);
                    ((ServerListAdapter) lv_servers.getAdapter()).notifyDataSetChanged();
                    resetVideoView(true);
                }
            }
        });


        //初始化左右抽屉菜单列表
        lv_servers.setAdapter(new ServerListAdapter(MainActivity.this, serversList));

        //setListViewHeightBasedOnChildren(lv_programsType);
        setListViewHeightBasedOnChildren(lv_servers);

         */
    }

    CompoundButton.OnCheckedChangeListener cb_onlySoundOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            isOnlySound = b;
            resetVideoView(true);
        }
    };

    View.OnClickListener bt_playOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            resetVideoView(!videoView.isPlaying());
        }
    };

    /*
    退出流程设计
    若为打电话或按HOME键
        执行 onStop 事件后不执行 onDestory
        在返回时 不执行 onCreate ，而是执行 onSavedInstanceState。onRestart()开始-onStart()-onResume()

    若为back键
        finish前台的activity，即activity的状态为onDestory为止
        再次启动该activity则从onCreate开始，不会调用onSavedInstanceState方法
     */

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(isPlayingResume)
            resetVideoView(true);
    }

    @Override
    protected void onPause() {
        if(videoView.isPlaying()) {
            isPlayingResume =true;
            resetVideoView(false);
        } else {
            isPlayingResume =false;
        }
        super.onPause();
    }


    /**
     * 网络连接失败后关闭程序
     */
    void networkFailClose(){
        Toast.makeText(this, R.string.wljwl, Toast.LENGTH_LONG).show();//提示信息
        MyLog.v("onCreate", getString(R.string.wljwl));
        //提示过信息5秒后关闭程序
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(8000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //没有连接到网络，关系程序
                finish();
            }
        }).start();
    }

    MediaPlayer.OnPreparedListener videoViewOnPreparedListener=new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            //缓冲结束
            if(proBar.getVisibility()==View.VISIBLE) proBar.setVisibility(View.INVISIBLE);
            if(!isOnlySound)videoView.setBackgroundResource(0);
            errTimes=0;
            VideoViewBuffering vb = new VideoViewBuffering();
            vb.start();
        }
    };

    MediaPlayer.OnErrorListener videoViewOnErrorListener=new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
            //region 错误信息翻译说明
                    /*
                    错误常数

MEDIA_ERROR_IO
文件不存在或错误，或网络不可访问错误
值: -1004 (0xfffffc14)

MEDIA_ERROR_MALFORMED
流不符合有关标准或文件的编码规范
值: -1007 (0xfffffc11)

MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK
视频流及其容器不适用于连续播放视频的指标（例如：MOOV原子）不在文件的开始.
值: 200 (0x000000c8)

MEDIA_ERROR_SERVER_DIED
媒体服务器挂掉了。此时，程序必须释放MediaPlayer 对象，并重新new 一个新的。
值: 100 (0x00000064)

MEDIA_ERROR_TIMED_OUT
一些操作使用了过长的时间，也就是超时了，通常是超过了3-5秒
值: -110 (0xffffff92)

MEDIA_ERROR_UNKNOWN
未知错误
值: 1 (0x00000001)

MEDIA_ERROR_UNSUPPORTED
比特流符合相关编码标准或文件的规格，但媒体框架不支持此功能
值: -1010 (0xfffffc0e)


what 	int: the type of error that has occurred:
    MEDIA_ERROR_UNKNOWN
    MEDIA_ERROR_SERVER_DIED
extra 	int: an extra code, specific to the error. Typically implementation dependent.
    MEDIA_ERROR_IO
    MEDIA_ERROR_MALFORMED
    MEDIA_ERROR_UNSUPPORTED
    MEDIA_ERROR_TIMED_OUT
    MEDIA_ERROR_SYSTEM (-2147483648) - low-level system error.

* */
            //endregion

            MyLog.e("MediaPlayer onError", "int what "+what+", int extra"+extra);


            //根据不同的错误进行信息提示
            if(what==MediaPlayer.MEDIA_ERROR_SERVER_DIED){
                //媒体服务器挂掉了。此时，程序必须释放MediaPlayer 对象，并重新new 一个新的。
                Toast.makeText(MainActivity.this, R.string.wlfwcw,
                        Toast.LENGTH_LONG).show();
            }else if(what==MediaPlayer.MEDIA_ERROR_UNKNOWN){
                if(extra==MediaPlayer.MEDIA_ERROR_IO){
                    //文件不存在或错误，或网络不可访问错误
                    Toast.makeText(MainActivity.this,R.string.wlljcw,
                            Toast.LENGTH_LONG).show();
                } else if(extra==MediaPlayer.MEDIA_ERROR_TIMED_OUT){
                    //超时
                    Toast.makeText(MainActivity.this,R.string.wlcs,
                            Toast.LENGTH_LONG).show();
                }
            }

            if(errTimes>3){
                errTimes=0;
                //发生错误，关闭播放的视频
                resetVideoView(false);
            }else {
                resetVideoView(true);
            }

            return false;
        }
    };

    View.OnTouchListener videoViewOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            //TODO 动画显示隐藏信息
            if(videoView.isPlaying()){
                if(menuManager.menuVisible){
                    menuManager.hideMenu();
                } else {
                    menuManager.displayMenu(true);
                }
            }
            return false;
        }
    };

    private void resetVideoView(boolean toPlay){

        if(proBar.getVisibility()==View.VISIBLE) proBar.setVisibility(View.INVISIBLE);

        if(!app.isNetworkConnected()){
            networkFailClose();
        }else {
            if (toPlay) {

                try {
                    videoView.stopPlayback();
                }catch (Exception e){}

                videoView.setVisibility(View.VISIBLE);
                videoView.setBackgroundResource(R.drawable.zcgt);

                proBar.setVisibility(View.VISIBLE);
                proBarThreadId++;
                //启动线程还控制进度控件的显示，当开始播放后，缓冲进度控件消失
                new ProBarThread(proBarThreadId).start();

                if(menuManager.menuVisible)
                    menuManager.delayHide();
                else
                    menuManager.displayMenu(true);

                if (isOnlySound) {
                    videoView.setVideoURI(getSoundUriAuto());
                } else {
                    videoView.setVideoURI(getVideoUriAuto());
                }
                videoView.start();
                bt_play.setImageResource(R.mipmap.ic_stop);
            } else {

                try {
                    videoView.stopPlayback();
                }catch (Exception e){}

                if(menuManager.menuVisible)
                    menuManager.alwaysShow();
                else
                    menuManager.displayMenu(false);

                videoView.setVisibility(View.VISIBLE);
                videoView.setBackgroundResource(R.drawable.zcgt);
                bt_play.setImageResource(R.mipmap.ic_play);
            }
        }
    }

    public Uri getSoundUriAuto(){
        String url = channel.audioUrl;
        Uri uri = Uri.parse(url.replace("server", serversList.getSelectedServer().domain));
        return uri;
    }

    public Uri getVideoUriAuto(){
        String url = channel.tvUrl;
        Uri uri = Uri.parse(url.replace("server", serversList.getSelectedServer().domain));
        return uri;
    }

    /**
     * 计算弄表高度
     * @param listView
     */
    public void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    //监视视频播放过种中是否存在暂停缓冲的线程，如果视频暂停缓冲，则等待50秒，到时还未播放则报错
    class VideoViewBuffering extends Thread {
        int old_duration = -1;
        int isBuffering = 0;//0 无proBar 1 要求设置显示proBar 2 已设置显示proBar
        int bufferingC = 0;
        public void run() {
            old_duration = -1;
            isBuffering = 0;
            bufferingC = 0;

            //视频没有开始播放则一直等待
            while (!videoView.isPlaying()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //等待50秒，若50秒没播放则报错
            //视频播放中，则开始检测状态
            while (videoView.isPlaying()){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //未播放或已经停止则返回
                if(videoView==null || !videoView.isPlaying()){
                    break;
                }

                //获取当前进度
                int duration = videoView.getCurrentPosition();

                //如果新的进度和老的进度相等，则说明视频暂停了，处于缓冲状态，否则又开始播放了
                if (old_duration == duration && videoView.isPlaying()) {
                    //第一次进入缓冲暂停，则显示等等进度
                    if(isBuffering<1){
                        MyLog.i("VideoViewBuffering", "开始缓冲");
                        isBuffering = 1;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                proBar.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                    //如果超过10秒仍未播放则重启播放
                    bufferingC++;
                    if(bufferingC>100) {
                        MyLog.i("VideoViewBuffering", "缓冲超过50秒，重新开始播放");
                        //超过50秒后则重新启动
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                resetVideoView(true);
                            }
                        });
                        return;
                    }
                } else if(isBuffering>=1){
                    //又开始播放了
                    MyLog.i("VideoViewBuffering", "缓冲完成，继续播放");
                    bufferingC=0;
                    isBuffering=0;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            proBar.setVisibility(View.INVISIBLE);
                        }
                    });
                }

                old_duration = duration;
                //MyLog.i("VideoViewBuffering", String.valueOf(duration));
            }

            MyLog.i("VideoViewBuffering", "跳出循环");
            if(isBuffering>=1 && !videoView.isPlaying()){
                MyLog.i("VideoViewBuffering", "停止播放，结束缓冲");
                bufferingC=0;
                isBuffering=0;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        proBar.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }
    }

    //点开始播放后等等缓冲结束，仅在播放前缓冲时运行。
    class ProBarThread extends Thread
    {
        private int tid;
        public ProBarThread(int tid)
        {
            this.tid = tid;
        }

        //等待50秒，若50秒没播放则报错
        public void run() {
            int i = 0;
            while (!videoView.isPlaying()) {

                if(proBarThreadId!=tid)
                    return;

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
                if (i > 500)
                    break;
            }

            //开始了其它视频加载
            if(proBarThreadId!=tid)
                return;

            //视频加载失败
            if (!videoView.isPlaying()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (proBar.getVisibility() == View.VISIBLE) {
                            resetVideoView(false);
                            Toast.makeText(MainActivity.this, R.string.spjzsb,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }
    }
}
