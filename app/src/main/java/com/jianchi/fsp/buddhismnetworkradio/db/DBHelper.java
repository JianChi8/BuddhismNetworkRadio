package com.jianchi.fsp.buddhismnetworkradio.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.alibaba.fastjson.JSON;
import com.jianchi.fsp.buddhismnetworkradio.tools.FileUtils;
import com.jianchi.fsp.buddhismnetworkradio.R;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3Program;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/1/23.
 */

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "rec.db";
    private static final int DATABASE_VERSION = 1;
    private static DBHelper helper;
    private Context context;

    private DBHelper(Context context) {
        //CursorFactory设置为null,使用默认值
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public static void init(Context context){
        helper = new DBHelper(context);
    }

    public static DBHelper getHelper(){ return helper;}

    /*
    数据库第一次被创建时onCreate会被调用
    用这一个数据库保存所有数据
    */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS [rec]" +
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT, type VARCHAR, key1 VARCHAR, key2 VARCHAR, info TEXT, updateTime INT64)");

        String json = FileUtils.readRawAllText(context, R.raw.item10);
        List<Mp3Program> mp3Programs = JSON.parseArray(json, Mp3Program.class);
        List<Rec> recList = new ArrayList<>();
        for(Mp3Program mp3Program : mp3Programs){
            recList.add(mp3ToRec(mp3Program));
        }

        db.beginTransaction();	//开始事务
        try {
            for(Rec rec: recList){
                db.execSQL("INSERT INTO rec VALUES(null, ?, ?, ?, ?, ?)", new Object[]{rec.type, rec.key1, rec.key2, rec.info, rec.updateTime});
            }
            db.setTransactionSuccessful();	//设置事务成功完成
        } finally {
            db.endTransaction();	//结束事务
        }
    }

    //如果DATABASE_VERSION值被改为2,系统发现现有数据库版本不同,即会调用onUpgrade
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("ALTER TABLE person ADD COLUMN other STRING");
    }

    private Rec mp3ToRec(Mp3Program newsMessage) {
        Rec rec = new Rec();
        rec.type = "mp3rec";
        rec.key1 = String.valueOf(newsMessage.id);
        rec.key2 = newsMessage.name;
        rec.info = JSON.toJSONString(newsMessage);
        return rec;
    }
}
