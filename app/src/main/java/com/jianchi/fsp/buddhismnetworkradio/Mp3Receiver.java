package com.jianchi.fsp.buddhismnetworkradio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.lang.reflect.Method;

/**
 * Created by fsp on 17-8-12.
 */

public class Mp3Receiver extends BroadcastReceiver {

    IMusic mp3Biner;
    public Mp3Receiver(IMusic mp3Biner){
        this.mp3Biner = mp3Biner;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Mp3Biner.PLAY_PAUSE_BUTTON)) {
            //用context获取service，然后控制service来停止音乐
            mp3Biner.pause();
        }
    }
}
