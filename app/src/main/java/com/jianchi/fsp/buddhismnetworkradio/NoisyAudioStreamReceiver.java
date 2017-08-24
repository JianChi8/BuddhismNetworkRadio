package com.jianchi.fsp.buddhismnetworkradio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by fsp on 17-8-17.
 */

public class NoisyAudioStreamReceiver extends BroadcastReceiver {
    IMusic mp3Biner;
    public NoisyAudioStreamReceiver(IMusic mp3Biner){
        this.mp3Biner = mp3Biner;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        mp3Biner.pause();
    }
}
