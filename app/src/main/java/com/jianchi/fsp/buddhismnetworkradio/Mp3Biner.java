package com.jianchi.fsp.buddhismnetworkradio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.jianchi.fsp.buddhismnetworkradio.activity.StartActivity;
import com.jianchi.fsp.buddhismnetworkradio.db.Mp3RecDBManager;
import com.jianchi.fsp.buddhismnetworkradio.mp3.AudioPlayer;
import com.jianchi.fsp.buddhismnetworkradio.mp3.FtpServer;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3DownloadThread;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3Program;

import java.io.File;

/**
 * 状态栏更新规则：开始播放时创建状态栏、播放下一首更新状态栏，暂停时销毁状态栏，当服务销毁时保存进度并销毁状态栏
 * 是否播放均是控制器在控制，这里用到music播放控制只有三种情况：1、刚开始，2、下一首，3、点了状态栏暂停
 * 控制可产生的动作：播放、暂停，快进快退
 *
 * 若要加入播放本地文件，保存功能需要做较大的更改，需要对远程mp3和文件进度分另保存
 */

public class Mp3Biner extends Binder implements IMusic  {
    public static final String StartWith_REMOTE_MP3_PLAYER_SERVICE= "StartWith_REMOTE_MP3_PLAYER_SERVICE";
    public static final String StartWith_LOCAL_MP3_PLAYER_SERVICE= "StartWith_LOCAL_MP3_PLAYER_SERVICE";

    /**
     * 上下文件，用来创建Notification
     */
    Context context;


    /**
     * 音乐播放器
     */
    AudioPlayer mp;

    /*
    是不是本地文件
     */
    public boolean isLocal = false;

    /**
     * 播放记录
     */
    Mp3Program mp3;


    /**
     * 通知
     */
    private Notification notification;

    /**
     * 通知中按扭点击ID
     */
    public static final String PLAY_PAUSE_BUTTON = "com.notifications.intent.action.PlayPauseButtonClick";

    /**
     * 通知ID
     */
    public static final int NOTI_CTRL_ID = 25478;

    /**
     * 通知外面的listView来更新ListView
     */
    NotifyEventListener notifyEventListener;



    /**
     * 歌曲列表，仅在播放远程mp3列表时使用，本地mp3播放为单文件，所以不用
     */
    String[] mp3s;

    /**
     * FTP 服务器信息，本地播放不需要
     */
    FtpServer server;

    /**
     * 当前播放音乐在列表中的位置，仅在播放器状态更改事件中赋值，用于标记是否更换了播放文件
     * 也仅在播放远程mp3列表时使用，本地mp3播放为单文件，所以不用
     */
    int mp3Id;



    public Mp3Biner(Context context){
        this.context = context;
        mp = new AudioPlayer(context, playerEventListener);
        mp3Id=-1;
        mp3=null;
        server=null;
        notification=null;
        notifyEventListener=null;
    }

    /**
     * 初始化远程mp3播放列表数据，若以前有数据并为播放状态，则保存数据
     * @param mp3s
     * @param server
     * @param mp3
     */
    @Override
    public void initMp3Data(String[] mp3s, FtpServer server, Mp3Program mp3) {
        if(this.mp3!=null && !this.mp3.id.equals(mp3.id)){
            //这时更换了节目单，如果它还在播放则停止并保存
            if(mp.isPlaying()){
                //停止播放
                mp.pause();
            }
        }

        isLocal = false;
        //在状态变更中赋值，这里标记为-1
        mp3Id = -1;

        this.server = server;
        this.mp3s = mp3s;
        this.mp3 = mp3;

        //查询序号
        int windowIndex = 0;
        for(int i=0; i<mp3s.length; i++){
            if(mp3.curPlayFile.equals(mp3s[i])){
                windowIndex=i;
                break;
            }
        }
        long positionMs = mp3.postion;

        String[] urls = new String[mp3s.length];
        for(int i=0; i<mp3s.length; i++)
            urls[i] = "ftp://"+server.server+ Mp3DownloadThread.getFtpPath(server, mp3.id) + mp3.curPlayFile;

        //开始播放
        mp.play(urls, windowIndex, positionMs);
    }


    /**
     * 初始化数据，若以前有数据并为播放状态，则保存数据
     * @param mp3File
     */
    @Override
    public void play(File mp3File, Mp3Program mp3) {

        //这时更换了节目单，如果它还在播放则停止并保存
        if (mp.isPlaying()) {
            //停止播放
            mp.pause();
        }

        isLocal = true;
        this.mp3 = mp3;
        mp3.curPlayFile = mp3File.getName();

        //本地播放用不到这两个变量
        this.server = null;
        mp3Id = -1;
        this.mp3s = null;

        mp.play(mp3File, mp3.postion);
    }

    /**
     * 暂停，并保存进度
     */
    @Override
    public void pause() {
        mp.pause();
    }

    /**
     * 播放指定歌曲
     */
    @Override
    public void moveon(int playMp3Id, int pos) {
        mp.getPlayer().seekTo(playMp3Id, pos);
        mp.getPlayer().setPlayWhenReady(true);
    }


    /**
     * 停止，仅在销毁服务时停止，其它停止操作均为 pause
     */
    @Override
    public void stop() {
        mp.getPlayer().setPlayWhenReady(false);
        mp.getPlayer().stop();
        mp.release();
        mp = null;

        cancelNotification();
    }

    @Override
    public void reset() {
        stop();

        mp3Id=-1;
        mp3=null;
        server=null;

        cancelNotification();

        notification=null;
        notifyEventListener=null;

        mp = new AudioPlayer(context, playerEventListener);
    }

    @Override
    public String[] getMp3s() {
        return this.mp3s;
    }

    @Override
    public Mp3Program getMp3Program() {
        return mp3;
    }

    @Override
    public ExoPlayer getPlayer() {
        return mp.getPlayer();
    }

    /**
     * 设置更新提醒事件
     * @param listener
     */
    @Override
    public void setNotifyEventListener(NotifyEventListener listener) {
        this.notifyEventListener = listener;
    }

    /**
     * 取消状态栏提醒
     */
    private void cancelNotification(){
        if(notification!=null) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTI_CTRL_ID);
            notification = null;
        }
    }

    /**
     *
     */
    ExoPlayer.EventListener playerEventListener =  new ExoPlayer.EventListener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

            //mp.getPlayer().getPlaybackState();

            /**
             * The player does not have a source to play, so it is neither buffering nor ready to play.
             */
            int STATE_IDLE = 1;
            /**
             * The player not able to immediately play from the current position. The cause is
             * {@link Renderer} specific, but this state typically occurs when more data needs to be
             * loaded to be ready to play, or more data needs to be buffered for playback to resume.
             */
            int STATE_BUFFERING = 2;
            /**
             * The player is able to immediately play from the current position. The player will be playing if
             * {@link #getPlayWhenReady()} returns true, and paused otherwise.
             */
            int STATE_READY = 3;
            /**
             * The player has finished playing the media.
             */
            int STATE_ENDED = 4;


            //事件分成两大类：播放和暂停
            if(playWhenReady){
                //根据是否开始缓存和缓存结束，决定是否显示等待进度对话框
                if(playbackState==STATE_BUFFERING){
                    if(notifyEventListener!=null)
                        notifyEventListener.proBar(true);
                }
                if(playbackState==STATE_READY){
                    if(notifyEventListener!=null)
                        notifyEventListener.proBar(false);
                }

                //开始播放或由暂停转为播放时产生此事件
                if(playbackState==STATE_BUFFERING || playbackState==STATE_READY){
                    if(isLocal) {
                        initNotificationBar(mp3.name, mp3.curPlayFile);
                        if (notifyEventListener != null)
                            notifyEventListener.handleEvent();
                    } else {
                        //更换文件是，必然要缓存，可于此时显示状态栏
                        int idx = mp.getPlayer().getCurrentWindowIndex();
                        if (mp3Id != idx) {
                            mp3Id = idx;//在全局变量中标记当前播放位置
                            mp3.curPlayFile = mp3s[mp3Id];
                            long pos = mp.getPlayer().getCurrentPosition();
                            mp3.postion = (int) pos;

                            initNotificationBar(mp3.name, mp3.curPlayFile);
                            if (notifyEventListener != null)
                                notifyEventListener.handleEvent();
                        }
                    }
                } else if(playbackState == STATE_ENDED){
                    //当前文件播放结束时产生些事件
                    if (notifyEventListener != null)
                        notifyEventListener.curMp3PlayOver();
                }
            } else {
                if(playbackState==STATE_BUFFERING || playbackState==STATE_READY){
                    //暂停
                    if(isLocal){
                        mp3.postion = (int) mp.getPlayer().getCurrentPosition();//.getDuration();
                        saveLocalProcess((int) mp.getPlayer().getCurrentPosition());

                        if (notifyEventListener != null)
                            notifyEventListener.proBar(false);

                        //更新状态栏
                        cancelNotification();
                    } else {
                        //暂停了，这时要保存进度的
                        int idx = mp.getPlayer().getCurrentWindowIndex();
                        mp3.curPlayFile = mp3s[idx];
                        mp3.postion = (int) mp.getPlayer().getCurrentPosition();//.getDuration();

                        Log.d("SaveProcess", mp3.curPlayFile + ":" + mp3.postion);

                        saveRemoteProcess();

                        if (notifyEventListener != null)
                            notifyEventListener.proBar(false);

                        mp3Id = -1;

                        //更新状态栏
                        cancelNotification();
                    }
                }
            }
        }

        /**
         * 没有改变时间线而发生了中断，在这里捕获自动播放跳到了下一个文件的事件
         */
        @Override
        public void onPositionDiscontinuity() {
            if(isLocal){
                //因为是单文件播放，不会跳到下一个文件，不用管这个
            } else {
                int idx = mp.getPlayer().getCurrentWindowIndex();
                if (mp3Id != idx) {
                    //播放到了下一个文件了
                    //自动跳到下一个文件，在onPlayerStateChanged中没有相关事件
                    mp3Id = idx;//在全局变量中标记当前播放位置
                    mp3.curPlayFile = mp3s[mp3Id];
                    long pos = mp.getPlayer().getCurrentPosition();
                    mp3.postion = (int) pos;

                    initNotificationBar(mp3.name, mp3.curPlayFile);
                    if (notifyEventListener != null)
                        notifyEventListener.handleEvent();
                }
            }
        }

        /**
         * Called when an error occurs
         */
        @Override
        public void onPlayerError(ExoPlaybackException error) {
        }


        /**
         * 时间线发生变化
         */
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {

        }

        /**
         * 改变了音轨
         */
        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        /**
         * Called when the player starts or stops loading the source.
         */
        @Override
        public void onLoadingChanged(boolean isLoading) {
        }

        /**
         * 当播放参数改变时，比如播放的速度
         * */
        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        }
    };

    /**
     * 在暂停或更换了节目表时保存
     */
    private void saveLocalProcess(int postion) {
        //步骤2-1：创建一个SharedPreferences.Editor接口对象，lock表示要写入的XML文件名，MODE_WORLD_WRITEABLE写操作
        SharedPreferences.Editor editor = context.getSharedPreferences("localmp3process", Context.MODE_PRIVATE).edit();
        editor.putInt("postion", postion);
        editor.commit();
    }

    /**
     * 在暂停或更换了节目表时保存
     */
    private void saveRemoteProcess() {

        Mp3RecDBManager db = new Mp3RecDBManager();
        if (mp3.dbRecId == -1) {
            mp3.dbRecId = db.add(mp3);
        } else {
            db.update(mp3);
        }
    }

    RemoteViews remoteViews;
    NotificationManager notificationManager;
    /*
    这里要进行状态栏初始化
     */
    private void initNotificationBar(String name, String msg) {
        if(notification==null) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
            //RemoteViews
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.item_notification);

            remoteViews.setImageViewResource(R.id.imageView, R.mipmap.ic_launcher);
            remoteViews.setTextViewText(R.id.textView, name);
            remoteViews.setTextViewText(R.id.mp3_file_name, msg);

            Intent intent = new Intent(context, StartActivity.class);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setAction(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            if(isLocal)
                intent.putExtra("StartWith", StartWith_LOCAL_MP3_PLAYER_SERVICE);
            else
                intent.putExtra("StartWith", StartWith_REMOTE_MP3_PLAYER_SERVICE);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

            Intent buttonIntent = new Intent(PLAY_PAUSE_BUTTON);
            PendingIntent intent_stop = PendingIntent.getBroadcast(context, 1, buttonIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.notify_stop_mp3, intent_stop);

            mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                    .setTicker(name)
                    .setOngoing(true)
                    .setContentIntent(contentIntent)
                    .setContent(remoteViews);
            notification = mBuilder.build();

            notification.flags = notification.FLAG_NO_CLEAR;//设置通知点击或滑动时不被清除
            notificationManager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTI_CTRL_ID, notification);//开启通知

        } else {
            remoteViews.setTextViewText(R.id.textView, name);
            remoteViews.setTextViewText(R.id.mp3_file_name, msg);
            mHander.sendEmptyMessage(1);
        }
    }

    private Handler mHander = new Handler(){
        public void handleMessage(android.os.Message msg) {
            notificationManager.notify(NOTI_CTRL_ID, notification);
        };
    };
}
