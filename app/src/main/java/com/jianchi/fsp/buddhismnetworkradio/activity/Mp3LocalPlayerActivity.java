package com.jianchi.fsp.buddhismnetworkradio.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.jianchi.fsp.buddhismnetworkradio.BApplication;
import com.jianchi.fsp.buddhismnetworkradio.Mp3Biner;
import com.jianchi.fsp.buddhismnetworkradio.NotifyEventListener;
import com.jianchi.fsp.buddhismnetworkradio.R;
import com.jianchi.fsp.buddhismnetworkradio.adapter.Mp3LocalListAdapter;
import com.jianchi.fsp.buddhismnetworkradio.mp3.DownloadEvenListener;
import com.jianchi.fsp.buddhismnetworkradio.mp3.DownloadStatus;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3DownloadThread;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3File;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3Program;
import com.jianchi.fsp.buddhismnetworkradio.tools.TW2CN;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 执行过程：
 * 1、读取保存的项目
 * 2、若没有，则为空，什么也不做
 */
public class Mp3LocalPlayerActivity extends AppCompatActivity {

    public static final int ADD_LOCAL_MP3 = 2558;

    ListView lv;
    BApplication app;
    Mp3Program mp3Program;
    int maxDownloadFiles;
    boolean onlyWifi;

    List<Mp3File> mp3s;

    PlaybackControlView playbackControlView;
    Mp3LocalListAdapter mp3ListAdapter;

    //用于标记上一个文件的序号
    int preMp3PlayId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mp3_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        app = (BApplication)getApplication();

        lv = (ListView) findViewById(R.id.lv_mp3);
        lv.setOnItemClickListener(onItemClickListener);

        playbackControlView = (PlaybackControlView) findViewById(R.id.playbackControlView);
        playbackControlView.setShowTimeoutMs(0);
        playbackControlView.show();

        //由无程播放转过来，初始化播放器
        if(app.music.getMp3Program()!=null && app.music.getMp3Program().dbRecId !=-1)
            app.music.reset();

        playbackControlView.setPlayer(app.music.getPlayer());

        app.music.setNotifyEventListener(notifyEventListener);

        Intent intent = getIntent();
        String startWith = intent.getStringExtra("StartWith");


        if(startWith!=null && startWith.equals(Mp3Biner.StartWith_REMOTE_MP3_PLAYER_SERVICE)) {
            loadFromServiceAndDownloadThread();
        } else if(app.music.getMp3Program()!=null && app.music.getMp3Program().dbRecId ==-1){
            loadFromServiceAndDownloadThread();
        } else {
            SharedPreferences sharedPreferences = getSharedPreferences("localmp3process", Context.MODE_PRIVATE);
            if (sharedPreferences.contains("id")) {
                loadMp3();
            } else {
                //显示信息
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setMessage(R.string.local_mp3_explain)
                        .setPositiveButton("OK",null)
                        .create();
                dialog.show();
            }
        }

        if(app.mp3DownloadThread!=null)
            app.mp3DownloadThread.setDownloadEvenListener(downloadEvenListener);
    }

    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //播放单个文件或下载单个文件
            if(preMp3PlayId!=position) {
                //
                Mp3File mp3 = mp3s.get(position);
                File mp3File = new File(app.mp3DownloadThread.localMp3Dir, mp3.fileName);

                //更新上一个的状态为已下载，也可能没下载完就变更了
                if(preMp3PlayId!=-1){
                    mp3s.get(preMp3PlayId).state = Mp3File.LocaleMp3State.Downloaded;
                }

                if (mp3File.exists()) {
                    //更新上一个播放ID为本ID，并设置状态为播放
                    preMp3PlayId = position;
                    mp3s.get(preMp3PlayId).state = Mp3File.LocaleMp3State.Playing;

                    //mp3Program为播放项目，并在播放服务中相同，
                    //若还是当前文件，则直接播放
                    if (mp3Program.curPlayFile.equals(mp3.fileName)) {
                        app.music.play(mp3File, mp3Program);
                    } else {
                        //不是当前文件了，重新生成一个mp3Program，这里dbid=-1，用此来标记是不是由数据库中取出，由数据库中取出的为远程播放
                        //播放当前文件
                        mp3Program.curPlayFile = mp3.fileName;
                        mp3Program.postion = 0;
                        //保存
                        SharedPreferences.Editor editor = getSharedPreferences("localmp3process", Context.MODE_PRIVATE).edit();
                        editor.putString("curPlayFile", mp3Program.curPlayFile);
                        editor.putInt("postion", 0);
                        editor.commit();

                        app.music.play(mp3File, mp3Program);
                    }
                } else {
                    //文件没有下载完，不能播放
                    preMp3PlayId = -1;
                    app.music.reset();
                    playbackControlView.setPlayer(app.music.getPlayer());

                    SharedPreferences.Editor editor = getSharedPreferences("localmp3process", Context.MODE_PRIVATE).edit();
                    editor.putString("curPlayFile", mp3.fileName);
                    editor.putInt("postion", 0);
                    editor.commit();
                }

                //开启下载
                app.mp3DownloadThread.start(position);
                //mp3ListAdapter.notifyDataSetChanged();
            }
        }
    };

    private void loadFromServiceAndDownloadThread(){
        //载入数据
        mp3Program = app.music.getMp3Program();
        if(mp3Program==null)
        {
            SharedPreferences sharedPreferences = getSharedPreferences("localmp3process", Context.MODE_PRIVATE);
            mp3Program = new Mp3Program();
            mp3Program.curPlayFile = sharedPreferences.getString("curPlayFile", "");
            mp3Program.id = sharedPreferences.getString("id", "");
            mp3Program.postion = sharedPreferences.getInt("postion", 0);
            mp3Program.name = sharedPreferences.getString("name", "");
        }

        maxDownloadFiles = app.mp3DownloadThread.maxDownloadFiles;
        onlyWifi = app.mp3DownloadThread.onlyWifi;
        mp3s = app.mp3DownloadThread.mp3s;

        mp3ListAdapter = new Mp3LocalListAdapter(this, mp3s);
        lv.setAdapter(mp3ListAdapter);


        setTitle(TW2CN.getInstance(this).toLocalString(mp3Program.name));

        preMp3PlayId = app.mp3DownloadThread.curPlayMp3sIdx;

        //app.mp3DownloadThread.setDownloadEvenListener(downloadEvenListener);
        //app.mp3DownloadThread.start(app.mp3DownloadThread.curPlayMp3sIdx);
        /*
        if(app.music.getPlayer().getPlayWhenReady()) {
            if(!app.music.getMp3Program().curPlayFile.isEmpty()){
                //
            }
        }
        */
    }

    private void loadMp3() {

        SharedPreferences sharedPreferences = getSharedPreferences("localmp3process", Context.MODE_PRIVATE);

        mp3Program = new Mp3Program();
        mp3Program.curPlayFile = sharedPreferences.getString("curPlayFile", "");
        mp3Program.id = sharedPreferences.getString("id", "");
        mp3Program.postion = sharedPreferences.getInt("postion", 0);
        mp3Program.name = sharedPreferences.getString("name", "");
        maxDownloadFiles = sharedPreferences.getInt("maxDownloadFiles", 5);
        onlyWifi = sharedPreferences.getBoolean("onlyWifi", true);

        if (!mp3Program.id.isEmpty()) {
            if(app.mp3DownloadThread!=null && app.mp3DownloadThread.mp3ProgramId.equals(mp3Program.id)){
                mp3s = app.mp3DownloadThread.mp3s;
            } else {
                mp3s = new ArrayList<>();
                String[] mp3ps = mp3Program.queryMp3Files(this);
                for (int i = 0; i < mp3ps.length; i++) {
                    Mp3File mp3File = new Mp3File();
                    mp3File.state = Mp3File.LocaleMp3State.NoDownload;
                    mp3File.fileName = mp3ps[i];
                    mp3File.url = mp3Program.id;
                    mp3s.add(mp3File);
                }
            }

            setTitle(TW2CN.getInstance(this).toLocalString(mp3Program.name));

            if (app.mp3DownloadThread == null) {
                app.mp3DownloadThread = new Mp3DownloadThread(this, mp3s, app.fastFtpServer, maxDownloadFiles, onlyWifi, downloadEvenListener, mp3Program.id);
            } else if(!mp3Program.id.equals(app.mp3DownloadThread.mp3ProgramId)) {
                //app.mp3DownloadThread.setDownloadEvenListener(downloadEvenListener);
                app.mp3DownloadThread.setDownloadMp3(mp3s, mp3Program.id, 0);
                preMp3PlayId = -1;
                app.music.reset();
                playbackControlView.setPlayer(app.music.getPlayer());
            }

            int position = 0;
            if (mp3Program.curPlayFile.isEmpty()) {
                mp3Program.curPlayFile = mp3s.get(0).fileName;

                //保存
                SharedPreferences.Editor editor = getSharedPreferences("localmp3process", Context.MODE_PRIVATE).edit();
                editor.putString("curPlayFile", mp3Program.curPlayFile);
                editor.commit();
            } else {
                for (int i = 0; i < mp3s.size(); i++) {
                    if (mp3Program.curPlayFile.equals(mp3s.get(i).fileName)) {
                        position = i;
                        break;
                    }
                }
            }

            app.mp3DownloadThread.start(position);

            Mp3File mp3File = mp3s.get(position);
            File file = new File(app.mp3DownloadThread.localMp3Dir, mp3File.fileName);
            if (file.exists()) {
                preMp3PlayId = position;
                mp3s.get(preMp3PlayId).state = Mp3File.LocaleMp3State.Playing;
                if(app.music.getMp3Program()!=mp3Program)
                    app.music.play(file, mp3Program);
            } else {
                preMp3PlayId = -1;
                app.music.reset();
                playbackControlView.setPlayer(app.music.getPlayer());
            }

            mp3ListAdapter = new Mp3LocalListAdapter(this, mp3s);
            lv.setAdapter(mp3ListAdapter);

        }
    }

    NotifyEventListener notifyEventListener = new NotifyEventListener() {
        @Override
        public void handleEvent() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mp3ListAdapter!=null)
                        mp3ListAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void proBar(boolean show) {
        }

        @Override
        public void curMp3PlayOver() {
            //当前mp3播放完毕，可以播放下一个，暂时不做
        }
    };

    DownloadEvenListener downloadEvenListener = new DownloadEvenListener() {
        @Override
        public void handleEvent(final DownloadStatus status) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (status) {
                        case CONNECT_FAIL:
                            Toast.makeText(getApplicationContext(), R.string.down_msg_connect_fail, Toast.LENGTH_SHORT).show();
                            break;
                        case Remote_File_Noexist:
                            Toast.makeText(getApplicationContext(), R.string.down_msg_remote_file_noexist, Toast.LENGTH_SHORT).show();
                            break;
                        case Local_Bigger_Remote:
                            Toast.makeText(getApplicationContext(), R.string.down_msg_local_bigger_remote, Toast.LENGTH_SHORT).show();
                            break;
                        case Download_New_Success:
                            Toast.makeText(getApplicationContext(), R.string.down_msg_download_success, Toast.LENGTH_SHORT).show();
                            break;
                        case NoNextDownLoadMp3:
                            Toast.makeText(getApplicationContext(), R.string.down_msg_no_next, Toast.LENGTH_SHORT).show();
                            break;
                        case ReSetMp3State:
                            if(mp3ListAdapter!=null)
                                mp3ListAdapter.notifyDataSetChanged();
                            break;
                        case NoWifiNet:
                            Toast.makeText(getApplicationContext(), R.string.down_msg_no_wifi_net, Toast.LENGTH_SHORT).show();
                            break;
                        case WifiNetOk:
                            Toast.makeText(getApplicationContext(), R.string.down_msg_wifi_net_ok, Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            });
        }

        @Override
        public void updateProcess(long process, String fileName) {
            mp3ListAdapter.setProcess(process, fileName);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mp3ListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.local, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add_local_mp3_program) {
            Intent intent = new Intent(this, Mp3LocalManagerActivity.class);
            startActivityForResult(intent, ADD_LOCAL_MP3);
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) { //resultCode为回传的标记，我在B中回传的是RESULT_OK
            case RESULT_OK:
                if(requestCode==ADD_LOCAL_MP3){
                    //重新载入MP3 list
                    loadMp3();
                }
                break;
            default:
                break;
        }
    }
}
