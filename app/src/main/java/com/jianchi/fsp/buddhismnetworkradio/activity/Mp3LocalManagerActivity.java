package com.jianchi.fsp.buddhismnetworkradio.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;

import com.jianchi.fsp.buddhismnetworkradio.BApplication;
import com.jianchi.fsp.buddhismnetworkradio.R;
import com.jianchi.fsp.buddhismnetworkradio.adapter.Mp3LocalManagerAdapter;
import com.jianchi.fsp.buddhismnetworkradio.adapter.Mp3ManagerAdapter;
import com.jianchi.fsp.buddhismnetworkradio.db.Mp3RecDBManager;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3Program;

import java.util.ArrayList;
import java.util.List;

public class Mp3LocalManagerActivity extends AppCompatActivity {

    ExpandableListView lv;
    BApplication app;
    Mp3LocalManagerAdapter mp3ManagerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp3_manager);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //获取自定义APP，APP内存在着数据，若为旋转屏幕，此处记录以前的内容
        app = (BApplication)getApplication();

        lv = (ExpandableListView) findViewById(R.id.lv_mp3);
        lv.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Mp3Program mp3Program = (Mp3Program) v.getTag();
                if(!mp3Program.id.equals(mp3ManagerAdapter.selectedMp3Id)){
                    mp3ManagerAdapter.selectedMp3Id = mp3Program.id;
                    mp3ManagerAdapter.selectedMp3Name = mp3Program.name;
                    mp3ManagerAdapter.notifyDataSetChanged();
                }
                return false;
            }
        });

        SharedPreferences sharedPreferences = getSharedPreferences("localmp3process", Context.MODE_PRIVATE);
        String selectedMp3Id = sharedPreferences.getString("id", "");

        mp3ManagerAdapter = new Mp3LocalManagerAdapter(this, selectedMp3Id);
        lv.setAdapter(mp3ManagerAdapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            save();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void save(){

        SharedPreferences sharedPreferences = getSharedPreferences("localmp3process", Context.MODE_PRIVATE);
        String selectedMp3Id = sharedPreferences.getString("id", "");

        if(mp3ManagerAdapter.selectedMp3Id.equals(selectedMp3Id)) {
            setResult(RESULT_CANCELED);
        } else {
            //步骤2-1：创建一个SharedPreferences.Editor接口对象，lock表示要写入的XML文件名，MODE_WORLD_WRITEABLE写操作
            SharedPreferences.Editor editor = getSharedPreferences("localmp3process", Context.MODE_PRIVATE).edit();
            //步骤2-2：将获取过来的值放入文件
            editor.putString("curPlayFile", "");
            editor.putString("id", mp3ManagerAdapter.selectedMp3Id);
            editor.putString("name", mp3ManagerAdapter.selectedMp3Name);
            editor.putInt("postion", 0);
            //步骤3：提交
            editor.commit();
            setResult(RESULT_OK);
        }

        finish();
    }
}
