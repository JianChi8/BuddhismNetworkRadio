package com.jianchi.fsp.buddhismnetworkradio.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.jianchi.fsp.buddhismnetworkradio.R;
import com.jianchi.fsp.buddhismnetworkradio.api.Server;
import com.jianchi.fsp.buddhismnetworkradio.api.ServersList;
import com.jianchi.fsp.buddhismnetworkradio.tools.TW2CN;

/**
 * Created by fsp on 16-7-6.
 */
public class ServerListAdapter extends BaseAdapter {
    ServersList serversList;
    private LayoutInflater mInflater;
    Context context;
    public ServerListAdapter(Context context, ServersList serversList){
        this.context=context;
        this.serversList=serversList;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return serversList.servers.size();
    }

    @Override
    public Object getItem(int i) {
        return serversList.servers.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        Server holder = serversList.servers.get(i);
        Server selectedServer = serversList.getSelectedServer();
        //观察convertView随ListView滚动情况

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_servers, null);
        }
        convertView.setTag(holder);
        TextView txt = (TextView) convertView.findViewById(R.id.txt);
        txt.setText(TW2CN.getInstance(context).toLocal(holder.title));
        if(holder.title.equals(selectedServer.title)){
            txt.setTextColor(Color.parseColor("#ffff8800"));
        } else {
            txt.setTextColor(Color.BLACK);
        }

        return convertView;
    }
}
