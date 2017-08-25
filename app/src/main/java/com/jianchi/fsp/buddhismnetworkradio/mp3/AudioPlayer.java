package com.jianchi.fsp.buddhismnetworkradio.mp3;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;

/**
 * Created by fsp on 17-8-4.
 */

public class AudioPlayer {

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private Context mContext;
    private SimpleExoPlayer mPlayer;
    //private int state;
    private ExoPlayer.EventListener mEventListener;
    private ExtractorsFactory extractorsFactory;
    private boolean shouldAutoPlay;
    AudioManager am;
    public SimpleExoPlayer getPlayer() {
        return mPlayer;
    }

    public AudioPlayer(Context context, ExoPlayer.EventListener eventListener){
        shouldAutoPlay = true;
        mEventListener = eventListener;
        mContext = context;
        extractorsFactory = new DefaultExtractorsFactory();
        initializePlayer();
        am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        if(playWhenReady && !mPlayer.getPlayWhenReady()){
            //需要设置开始播放
            int result = am.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mPlayer.setPlayWhenReady(true);
            }
        } else if(!playWhenReady && mPlayer.getPlayWhenReady()) {
            //需要设置暂停
            if (mPlayer.getPlayWhenReady()) {
                mPlayer.setPlayWhenReady(false);
            }
            //am.abandonAudioFocus(afChangeListener); // Stop playback
        }
    }

    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {

            } else {
                if (mPlayer.getPlayWhenReady()) {
                    mPlayer.setPlayWhenReady(false);
                }
                //am.abandonAudioFocus(afChangeListener); // Stop playback
            }
/*
            else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                if (isPlaying()) {
                    mPlayer.setPlayWhenReady(false);
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            } else if (focusChange == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            }*/
        }
    };

    public void play(File file, int positionMs){

        FileDataSourceFactory fileDataSourceFactory = new FileDataSourceFactory(null);
            Uri uri = Uri.fromFile(file);
        MediaSource cmediaSource = new ExtractorMediaSource(uri,
                    fileDataSourceFactory,
                    extractorsFactory, null, null);

        setPlayWhenReady(true);
        mPlayer.seekTo(positionMs);
        mPlayer.prepare(cmediaSource, true, true);
    }

    public void play(String[] urls, int windowIndex, long positionMs){
        //state = 1;
        setMediaSource(urls);
        setPlayWhenReady(true);
        seekTo(windowIndex, positionMs);
    }

    private void setMediaSource(String[] audioUrls) {

        FtpDataSourceFactory mediaDataSourceFactory;
        LeastRecentlyUsedCacheEvictor leastRecentlyUsedCacheEvictor = new LeastRecentlyUsedCacheEvictor(FtpDataSourceFactory.cacheSize);
        Cache cache = new SimpleCache(new File(mContext.getExternalCacheDir(),"mp3cache"), leastRecentlyUsedCacheEvictor);

        mediaDataSourceFactory = new FtpDataSourceFactory(cache);

        MediaSource[] mediaSources = new MediaSource[audioUrls.length];
        for (int i = 0; i < audioUrls.length; i++) {
            Uri uri = Uri.parse(audioUrls[i]);

            mediaSources[i] = new ExtractorMediaSource(uri,
                    mediaDataSourceFactory,
                    extractorsFactory, null, null);
        }

        MediaSource cmediaSource = mediaSources.length == 1 ? mediaSources[0]
                : new ConcatenatingMediaSource(mediaSources);

        mPlayer.prepare(cmediaSource, true, true);

    }

    public boolean isPaused() {
        return !mPlayer.getPlayWhenReady() || mPlayer.getPlaybackState()==ExoPlayer.STATE_IDLE;
    }

    public boolean isPlaying() {
        return mPlayer.getPlayWhenReady() && (mPlayer.getPlaybackState()==ExoPlayer.STATE_BUFFERING || mPlayer.getPlaybackState()==ExoPlayer.STATE_READY );
    }

    public void pause() {
        //state = 2;
        setPlayWhenReady(false);
    }

    public void play() {
        //state = 1;
        setPlayWhenReady(true);
    }

    public void release(){
        mPlayer.release();
    }

    private void initializePlayer() {
        if (mPlayer == null) {
            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
            DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
            RenderersFactory renderersFactory = new DefaultRenderersFactory(mContext);
            mPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector, new DefaultLoadControl());
            mPlayer.addListener(mEventListener);
        }
    }

    public long getDuration() {
        return mPlayer.getDuration();
    }

    public int getCurrentWindowIndex() {
        return mPlayer.getCurrentWindowIndex();
    }

    public long getCurrentPosition() {
        return mPlayer.getCurrentPosition();
    }

    public void seekTo(int playMp3Id, long pos) {
        mPlayer.seekTo(playMp3Id, pos);
    }

    public void stop() {
        mPlayer.stop();
    }
}
