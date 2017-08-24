package com.jianchi.fsp.buddhismnetworkradio.api;

import java.util.List;

/**
 * Created by fsp on 16-7-22.
 */
public class ChannelList {

    public String selectedChannelTitle;
    public ChannelType getSelectedChannelType(){
        return Enum.valueOf(ChannelType.class, selectedChannelTitle);
    }
    public List<Channel> channels;

    public Channel getChannelByTitle(String title){
        for(Channel c : channels)
            if(c.title.equals(title))
                return c;
        return null;
    }

    public Channel getSelectedChannel(){
        return getChannelByTitle(selectedChannelTitle);
    }
}
