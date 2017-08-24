package com.jianchi.fsp.buddhismnetworkradio;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;

public class BMp3Service extends Service implements AudioManager.OnAudioFocusChangeListener {

    Mp3Biner binder;

    public BMp3Service() {
    }


    @Override
    public void onCreate() {
        super.onCreate();
        binder = new Mp3Biner(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        binder.stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return binder;
    }



    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        if (binder.mp.isPlaying()) {
                            binder.pause();
                        }
                        break;
        }
    }
}
