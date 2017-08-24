package com.jianchi.fsp.buddhismnetworkradio.mp3;

/**
 * Created by fsp on 17-8-20.
 */

public interface DownloadEvenListener {
    void handleEvent(DownloadStatus status);
    void updateProcess(long process, String fileName);
}
