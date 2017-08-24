package com.jianchi.fsp.buddhismnetworkradio.api;

/**
 * Created by fsp on 16-7-21.
 */
public class Channel {
    public String title;
    public String tvUrl;
    public String audioUrl;

    public ChannelType getChannelType(){
        return Enum.valueOf(ChannelType.class, title);
    }

    public Channel(){}

    public Channel(String title, String tvUrl, String audioUrl){
        this.title=title;
        this.tvUrl=tvUrl;
        this.audioUrl=audioUrl;
    }
}
