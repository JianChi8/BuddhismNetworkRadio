package com.jianchi.fsp.buddhismnetworkradio.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fsp on 16-7-5.
 */
public class ServersList {
    public List<Server> servers;
    public String selectServerCode="";
    public String  selectCityCode="";
    private Server selectedServer = null;

    public ServersList(){
        servers = new ArrayList<>();
    }

    public Server getServerByName(String serverName){
        for(Server si : servers)
            if(si.title.equals(serverName))
                return si;
        return null;
    }

    public void setSelectedServer(Server s){
        selectedServer = s;
    }

    public Server getSelectedServer(){
        if(selectedServer==null)
            selectedServer = getServerAuto();
        return selectedServer;
    }

    public Server getServerAuto(){

        if(selectServerCode.equals("台湾")){
            return getServerByName("臺北");
        }else if(selectServerCode.equals("日本")){
            return getServerByName("日本");
        }else if(selectServerCode.equals("德国")){
            return getServerByName("德國");
        }else if(selectServerCode.equals("中国")){
            if(selectCityCode.equals("江苏")){ //-- 江蘇
                return getServerByName("江蘇");
            }else if(selectCityCode.equals("山东")){ //-- 山東
                return getServerByName("山東");
            }else{   //-- 北京
                return getServerByName("北京");
            }
        }else{	//-- default 北京
            return getServerByName("北京");
        }
    }
}
