package com.jianchi.fsp.buddhismnetworkradio.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;
import android.webkit.WebView;

import com.jianchi.fsp.buddhismnetworkradio.BApplication;
import com.jianchi.fsp.buddhismnetworkradio.R;
import com.jianchi.fsp.buddhismnetworkradio.api.ChannelType;
import com.jianchi.fsp.buddhismnetworkradio.tools.TW2CN;

public class ScheduleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        String type = intent.getStringExtra("type");

        toolbar.setTitle(TW2CN.getInstance(this).toLocalString(type+"節目時間"));

        WebView webView = (WebView) findViewById(R.id.webView);
        ChannelType channelType = Enum.valueOf(ChannelType.class, type);
        webView.loadUrl(BApplication.programsListUrlMap.get(channelType));
    }

}
