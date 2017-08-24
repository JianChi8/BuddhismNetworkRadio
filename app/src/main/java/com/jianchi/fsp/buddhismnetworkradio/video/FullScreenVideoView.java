package com.jianchi.fsp.buddhismnetworkradio.video;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.VideoView;

import com.jianchi.fsp.buddhismnetworkradio.tools.MyLog;

/**
 * Created by fsp on 16-7-13.
 */
public class FullScreenVideoView extends VideoView {

    public FullScreenVideoView(Context context) {
        super(context);
    }

    public FullScreenVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FullScreenVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //重点。
        int width = getDefaultSize(0, widthMeasureSpec);
        int height = getDefaultSize(0, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener l) {
        super.setOnPreparedListener(l);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public int getCurrentPosition() {
        synchronized (this) {
            try {
                return super.getCurrentPosition();
            } catch (Exception e){
                MyLog.i("FullScreenVideoView.getCurrentPosition", e.getMessage());
                return -2;
            }
        }
    }

    @Override
    public boolean isPlaying() {
        synchronized (this) {
            try {
                return super.isPlaying();
            } catch (Exception e){
                MyLog.i("FullScreenVideoView.isPlaying", e.getMessage());
                return true;
            }
        }
    }

}