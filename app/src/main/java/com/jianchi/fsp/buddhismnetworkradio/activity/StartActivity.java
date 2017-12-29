package com.jianchi.fsp.buddhismnetworkradio.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.beardedhen.androidbootstrap.BootstrapButton;
import com.jianchi.fsp.buddhismnetworkradio.BApplication;
import com.jianchi.fsp.buddhismnetworkradio.Mp3Biner;
import com.jianchi.fsp.buddhismnetworkradio.R;
import com.jianchi.fsp.buddhismnetworkradio.adapter.Mp3ChannelListAdapter;
import com.jianchi.fsp.buddhismnetworkradio.adapter.TvChannelListAdapter;
import com.jianchi.fsp.buddhismnetworkradio.api.Channel;
import com.jianchi.fsp.buddhismnetworkradio.api.ChannelList;
import com.jianchi.fsp.buddhismnetworkradio.api.IpInfo;
import com.jianchi.fsp.buddhismnetworkradio.db.Mp3RecDBManager;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3Program;
import com.jianchi.fsp.buddhismnetworkradio.tools.MyLog;
import com.jianchi.fsp.buddhismnetworkradio.upgrade.EasyDialog;
import com.jianchi.fsp.buddhismnetworkradio.upgrade.UpgradeHelper;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 起始活动窗口
 * 包含两个节目列表：电视节目，随机点播
 * 处理启动事务，根据传过来的参数 StartWith 判断是否要直接跳转，并在启动本地或远程播放器时传入参数 StartWith
 *
 */
public class StartActivity extends AppCompatActivity {
    /**
     * 接收返回管理点播MP3列表的标记
     */
    public static final int MANAGER_MP3_RESULT = 2548;
    BApplication app;//全局应用
    ProgressDialog proDialog;//IP查询时的等待对话框
    ListView lv_channel;//视频列表
    boolean isTvChannel;//视频标示

    BootstrapButton bt_tv;//切换为视频按扭
    BootstrapButton bt_mp3;//切换为音频点播列表按扭

    List<Mp3Program> mp3Programs;//音频节目列表
    ChannelList channelList;//视频节目列表

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_start);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //获取自定义APP，APP内存在着数据，若为旋转屏幕，此处记录以前的内容
        app = (BApplication)getApplication();

        //音频和视频的列表视频，listview，在点击时判断点的是音频还是视频
        lv_channel = (ListView) findViewById(R.id.lv_channel);
        lv_channel.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(isTvChannel){
                    Channel programType = (Channel) view.getTag();
                    channelList.selectedChannelTitle = programType.title;
                    Intent intent = new Intent(StartActivity.this, MainActivity.class);
                    intent.putExtra("title", programType.title);
                    intent.putExtra("audioUrl", programType.audioUrl);
                    intent.putExtra("tvUrl", programType.tvUrl);
                    startActivity(intent);
                } else {
                    Mp3Program mp3Program = (Mp3Program) view.getTag();
                    Intent intent = new Intent(StartActivity.this, Mp3PlayerActivity.class);
                    //启动时判断是否已经开始播放音频节目了，并传入不同的参数
                    intent.putExtra("mp3id", mp3Program.id);
                    startActivity(intent);
                }
            }
        });


        //载入音频节目列表数据，并排序
        Mp3RecDBManager db = new Mp3RecDBManager();
        mp3Programs = db.getAllMp3Rec();
        Collections.sort(mp3Programs, new Comparator<Mp3Program>() {
            @Override
            public int compare(Mp3Program o1, Mp3Program o2) {
                Integer o1n = o1.num;
                return -o1n.compareTo(o2.num);
            }
        });

        //载入视频节目列表
        channelList = new ChannelList();
        String json = app.readRawFile(R.raw.channels);
        channelList.channels= JSON.parseArray(json, Channel.class);
        channelList.selectedChannelTitle =channelList.channels.get(0).title;

        //默认初始为视频节目
        TvChannelListAdapter tvChannelListAdapter = new TvChannelListAdapter(StartActivity.this, channelList, app);
        lv_channel.setAdapter(tvChannelListAdapter);
        isTvChannel = true;

        //切换音频视频
        bt_tv = (BootstrapButton) findViewById(R.id.bt_tv);
        bt_mp3 = (BootstrapButton) findViewById(R.id.bt_mp3);
        bt_tv.setOnCheckedChangedListener(new BootstrapButton.OnCheckedChangedListener() {
            @Override
            public void OnCheckedChanged(BootstrapButton bootstrapButton, boolean isChecked) {
                if(isChecked){
                    TvChannelListAdapter tvChannelListAdapter = new TvChannelListAdapter(StartActivity.this, channelList, app);
                    lv_channel.setAdapter(tvChannelListAdapter);
                    isTvChannel = true;
                }
            }
        });
        bt_mp3.setOnCheckedChangedListener(new BootstrapButton.OnCheckedChangedListener() {
            @Override
            public void OnCheckedChanged(BootstrapButton bootstrapButton, boolean isChecked) {
                if(isChecked) {
                    Mp3ChannelListAdapter mp3ChannelListAdapter = new Mp3ChannelListAdapter(StartActivity.this, mp3Programs);
                    lv_channel.setAdapter(mp3ChannelListAdapter);
                    isTvChannel = false;
                }
            }
        });

        if(app.isNetworkConnected()) {
            initData();
        } else {
            //当无网络时应该打开本地MP3
            Intent mp3Intent = new Intent(this, Mp3LocalPlayerActivity.class);
            startActivity(mp3Intent);

            //后退也不会退回到这里了
            finish();
        }

        //判断是不是由点击状态栏启动，判断传进来的参数
        Intent intent = getIntent();
        String startWith = intent.getStringExtra("StartWith");
        if(startWith!=null && startWith.equals(Mp3Biner.StartWith_REMOTE_MP3_PLAYER_SERVICE)) {
            //查看是否服务存在，数据都已经初始化
            if (app.fastFtpServer != null && app.music != null && app.music.getMp3Program() != null) {
                //数据都在，跳转
                Intent mp3Intent = new Intent(StartActivity.this, Mp3PlayerActivity.class);
                mp3Intent.putExtra("StartWith", Mp3Biner.StartWith_REMOTE_MP3_PLAYER_SERVICE);
                startActivity(mp3Intent);
            }
        } else if(startWith!=null && startWith.equals(Mp3Biner.StartWith_LOCAL_MP3_PLAYER_SERVICE)){
            //查看是否服务存在，数据都已经初始化
            if (app.fastFtpServer != null && app.music != null && app.music.getMp3Program() != null) {
                //数据都在，跳转
                Intent mp3Intent = new Intent(this, Mp3LocalPlayerActivity.class);
                mp3Intent.putExtra("StartWith", Mp3Biner.StartWith_LOCAL_MP3_PLAYER_SERVICE);
                startActivity(mp3Intent);
            }
        }

        UpgradeHelper upgradeHelper = new UpgradeHelper.Builder(this)
                .setUpgradeUrl(getString(R.string.down_url))
                .setIsAboutChecking(false)//关于页面手动检测更新需要设置isAboutChecking(true), 启动时检测设为false
                .build(StartActivity.this);
        upgradeHelper.check();

        //new EasyDialog(upgradeInfoModel, config, mContext).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            Intent intent = new Intent(this, ContactActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_news) {
            Intent intent = new Intent(this, NewsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_manager_mp3){
            Intent intent = new Intent(this, Mp3ManagerActivity.class);
            startActivityForResult(intent, MANAGER_MP3_RESULT);
            return true;
        } else if (id == R.id.action_local_mp3){
            Intent intent = new Intent(this, Mp3LocalPlayerActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) { //resultCode为回传的标记，我在B中回传的是RESULT_OK
            case RESULT_OK:
                if(requestCode==MANAGER_MP3_RESULT){
                    //重新载入MP3 list

                    Mp3RecDBManager db = new Mp3RecDBManager();
                    mp3Programs = db.getAllMp3Rec();
                    Collections.sort(mp3Programs, new Comparator<Mp3Program>() {
                        @Override
                        public int compare(Mp3Program o1, Mp3Program o2) {
                            Integer o1n = o1.num;
                            return -o1n.compareTo(o2.num);
                        }
                    });

                    Mp3ChannelListAdapter mp3ChannelListAdapter = new Mp3ChannelListAdapter(StartActivity.this, mp3Programs);
                    lv_channel.setAdapter(mp3ChannelListAdapter);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 初始化数据，单线程初始化
     */
    void initData() {
        //首次运行，API未初始化
        // 打开等待初始化的对话框
        proDialog = ProgressDialog.show(StartActivity.this, getString(R.string.zrsj), getString(R.string.sjjzz));

        //开启等待初始化动画
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!app.beInit) {
                        IpInfo ipInfo = getIpInof();
                        app.selectServerCode = ipInfo.data.country;
                        app.selectCityCode = ipInfo.data.region.substring(0,2);
                        app.beInit=true;
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //结束等待
                            if (proDialog != null) proDialog.dismiss();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    MyLog.v("initData", "initData 失败。");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(StartActivity.this, R.string.sjhcsb, Toast.LENGTH_LONG).show();
                            try {
                                Thread.sleep(8000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            //数据下载不完整，关闭程序
                            if (proDialog != null) proDialog.dismiss();
                            finish();
                        }
                    }).start();
                }
            }

        }).start();

    }


    private String downHtml(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
                "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:54.0) Gecko/20100101 Firefox/54.0").build();
        Call call = client.newCall(request);

        try {
            Response response = call.execute();
            String html = new String(response.body().bytes(), "utf-8");
            return html;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public IpInfo getIpInof(){
        String html = downHtml("http://ip.taobao.com/service/getIpInfo2.php?ip=myip");
        IpInfo ipInfo = JSON.parseObject(html, IpInfo.class);
        /*{
    "code": 0,
    "data": {
        "country": "中国", 台湾 日本 德国
        "country_id": "CN",
        "area": "华中",
        "area_id": "400000",
        "region": "河南省", 江苏  山东
        "region_id": "410000",
        "city": "焦作市",
        "city_id": "410800",
        "county": "",
        "county_id": "-1",
        "isp": "联通",
        "isp_id": "100026",
        "ip": "42.234.54.167"
    }
}*/
        return ipInfo;
    }
}
