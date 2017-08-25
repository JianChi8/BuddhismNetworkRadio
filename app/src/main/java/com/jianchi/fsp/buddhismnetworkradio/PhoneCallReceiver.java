package com.jianchi.fsp.buddhismnetworkradio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Created by fsp on 17-8-25.
 */

public class PhoneCallReceiver  extends BroadcastReceiver {

    IMusic mp3Biner;
    public PhoneCallReceiver(IMusic mp3Biner){
        this.mp3Biner = mp3Biner;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        TelephonyManager telephony = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        int state = telephony.getCallState();
        String TAG = "PhoneCallReceiver";
        switch(state){
            case TelephonyManager.CALL_STATE_RINGING:
                mp3Biner.pause();
                Log.i(TAG, "[Broadcast]等待接电话="+phoneNumber);
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                Log.i(TAG, "[Broadcast]电话挂断="+phoneNumber);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                Log.i(TAG, "[Broadcast]通话中="+phoneNumber);
                break;
        }
    }
}
