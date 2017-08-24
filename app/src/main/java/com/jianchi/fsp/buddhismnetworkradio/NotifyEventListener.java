package com.jianchi.fsp.buddhismnetworkradio;

/**
 * Created by fsp on 17-8-10.
 */

public interface NotifyEventListener {
    void handleEvent();
    void proBar(boolean show);
    void curMp3PlayOver();
}
