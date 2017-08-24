package com.jianchi.fsp.buddhismnetworkradio.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;

import com.jianchi.fsp.buddhismnetworkradio.BApplication;
import com.jianchi.fsp.buddhismnetworkradio.R;
import com.jianchi.fsp.buddhismnetworkradio.adapter.Mp3ManagerAdapter;
import com.jianchi.fsp.buddhismnetworkradio.db.Mp3RecDBManager;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3Program;

import java.util.ArrayList;
import java.util.List;

public class Mp3ManagerActivity extends AppCompatActivity {

    ExpandableListView lv;
    BApplication app;
    Mp3ManagerAdapter mp3ManagerAdapter;
    List<Mp3Program> mp3Programs;
    List<Mp3Program> checkedMpsPrograms;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp3_manager);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //获取自定义APP，APP内存在着数据，若为旋转屏幕，此处记录以前的内容
        app = (BApplication)getApplication();


        Mp3RecDBManager db = new Mp3RecDBManager();
        mp3Programs = db.getAllMp3Rec();
        checkedMpsPrograms = new ArrayList<>();
        for(Mp3Program p : mp3Programs)
            checkedMpsPrograms.add(p);

        lv = (ExpandableListView) findViewById(R.id.lv_mp3);
        lv.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Mp3Program mp3Program = (Mp3Program) v.getTag();
                Mp3Program mp = checkMp3Program(mp3Program);
                if(mp==null){
                    checkedMpsPrograms.add(mp3Program);
                } else {
                    checkedMpsPrograms.remove(mp);
                }
                mp3ManagerAdapter.notifyDataSetChanged();
                return false;
            }
        });

        mp3ManagerAdapter = new Mp3ManagerAdapter(this, checkedMpsPrograms);
        lv.setAdapter(mp3ManagerAdapter);

    }

    private Mp3Program checkMp3Program(Mp3Program mp3Program){
        for(Mp3Program mp : checkedMpsPrograms){
            if(mp.id.equals(mp3Program.id))
                return mp;
        }
        return null;
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
        List<Mp3Program> delMp3Programs = new ArrayList<Mp3Program>();
        List<Mp3Program> addMpsPrograms = new ArrayList<Mp3Program>();
        boolean found = false;
        for(Mp3Program mp : checkedMpsPrograms){
            found = false;
            for(Mp3Program mp2 : mp3Programs){
                if(mp.id.equals(mp2.id)){
                    found=true;
                    break;
                }
            }
            if(!found){
                addMpsPrograms.add(mp);
            }
        }
        for(Mp3Program mp : mp3Programs){
            found = false;
            for(Mp3Program mp2 : checkedMpsPrograms){
                if(mp.id.equals(mp2.id)){
                    found=true;
                    break;
                }
            }
            if(!found){
                delMp3Programs.add(mp);
            }
        }

        if(addMpsPrograms.size()>0 || delMp3Programs.size()>0) {
            Mp3RecDBManager db = new Mp3RecDBManager();
            if (addMpsPrograms.size() > 0)
                db.addMp3s(addMpsPrograms);
            if (delMp3Programs.size() > 0) {
                for(Mp3Program mp3Program : delMp3Programs)
                    db.delMp3(mp3Program);
            }
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }
}
