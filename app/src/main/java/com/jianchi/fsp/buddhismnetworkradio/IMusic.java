package com.jianchi.fsp.buddhismnetworkradio;

import com.google.android.exoplayer2.ExoPlayer;
import com.jianchi.fsp.buddhismnetworkradio.mp3.FtpServer;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3Program;

import java.io.File;

/**
 * Created by fsp on 17-8-10.
 */

public interface IMusic {
    public void moveon(int mp3Id, int i);//继续
    public void pause();//暂停
    public void stop();//停止
    public void reset();
    public String[] getMp3s();
    public Mp3Program getMp3Program();
    public ExoPlayer getPlayer();
    public void initMp3Data(String[] mp3s, FtpServer server, Mp3Program mp3);
    public void setNotifyEventListener(NotifyEventListener listener);

    void play(File mp3File, Mp3Program mp3);
}
