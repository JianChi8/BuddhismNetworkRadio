package com.jianchi.fsp.buddhismnetworkradio.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.jianchi.fsp.buddhismnetworkradio.BApplication;
import com.jianchi.fsp.buddhismnetworkradio.Mp3Biner;
import com.jianchi.fsp.buddhismnetworkradio.NotifyEventListener;
import com.jianchi.fsp.buddhismnetworkradio.R;
import com.jianchi.fsp.buddhismnetworkradio.adapter.Mp3ListAdapter;
import com.jianchi.fsp.buddhismnetworkradio.db.Mp3RecDBManager;
import com.jianchi.fsp.buddhismnetworkradio.mp3.FtpServer;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3Program;
import com.jianchi.fsp.buddhismnetworkradio.tools.TW2CN;

public class Mp3PlayerActivity extends AppCompatActivity {

    ListView lv;
    BApplication app;
    Mp3Program mp3Program;
    String[] mp3s;
    PlaybackControlView playbackControlView;
    FtpServer ftpServer;
    Mp3ListAdapter mp3ListAdapter;
    ProgressBar proBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp3_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        proBar = (ProgressBar) findViewById(R.id.mp3ProBar);

        //获取传递过来的参数
        Intent intent = getIntent();

        app = (BApplication)getApplication();
        ftpServer = app.getFtpServer();

        //启动分为三种情况，前两种数据来自播放服务，第三种因为服务未初始化，需要初始始化数据
        String startWith = intent.getStringExtra("StartWith");
        if(startWith!=null && startWith.equals(Mp3Biner.StartWith_REMOTE_MP3_PLAYER_SERVICE)) {
            //查看是否服务存在，数据都已经初始化
            mp3Program = app.music.getMp3Program();
            mp3s = app.music.getMp3s();
        } else {
            String mp3id = intent.getStringExtra("mp3id");
            if(app.music.getMp3Program()!=null && app.music.getMp3Program().dbRecId!=-1 && app.music.getMp3Program().id.equals(mp3id)){
                //dbRecId == -1则不是从数据库初始化的，一定是本地音乐时才有这种情况
                //查看是否服务存在，数据都已经初始化
                mp3Program = app.music.getMp3Program();
                mp3s = app.music.getMp3s();
            } else {

                Mp3RecDBManager db = new Mp3RecDBManager();
                //mp3Program必不为null，因为这是点击这个才来到这里的
                mp3Program = db.getMp3RecByMp3Id(mp3id);

                mp3s = mp3Program.queryMp3Files(this);
                if (mp3Program.curPlayFile.isEmpty()) {
                    mp3Program.curPlayFile = mp3s[0];
                    mp3Program.postion = 0;
                }

                //在这里应该将ExoPlayer加到BMp3Service中，service中进行控制，在这里会开始播放MP3
                app.music.initMp3Data(mp3s, ftpServer, mp3Program);
            }
        }

        mp3ListAdapter = new Mp3ListAdapter(this, mp3s, mp3Program);

        lv = (ListView) findViewById(R.id.lv_mp3);
        lv.setAdapter(mp3ListAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                app.music.moveon(position, 0);
            }
        });

        setTitle(TW2CN.getInstance(this).toLocalString(mp3Program.name));

        playbackControlView = (PlaybackControlView) findViewById(R.id.playbackControlView);
        playbackControlView.setShowTimeoutMs(0);
        playbackControlView.show();
        playbackControlView.setPlayer(app.music.getPlayer());

        app.music.setNotifyEventListener(new NotifyEventListener() {
            @Override
            public void handleEvent() {
                Mp3PlayerActivity.this.mp3ListAdapter.notifyDataSetChanged();
            }

            @Override
            public void proBar(boolean show) {
                if(show){
                    proBar.setVisibility(View.VISIBLE);
                } else {
                    proBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void curMp3PlayOver() {

            }
        });
    }
}
