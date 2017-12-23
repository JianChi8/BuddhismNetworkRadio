package com.jianchi.fsp.buddhismnetworkradio.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.AwesomeTextView;
import com.beardedhen.androidbootstrap.BootstrapBadge;
import com.jianchi.fsp.buddhismnetworkradio.R;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3File;
import com.jianchi.fsp.buddhismnetworkradio.tools.TW2CN;

import java.util.List;

/**
 * Created by fsp on 16-7-6.
 */
public class Mp3LocalListAdapter extends BaseAdapter {
    public List<Mp3File> data;
    private LayoutInflater mInflater;
    Context context;

    long process;
    String processFileName;

    public Mp3LocalListAdapter(Context context, List<Mp3File> data){
        this.context=context;
        this.data=data;
        this.mInflater = LayoutInflater.from(context);
        this.process = -1;
        this.processFileName = "";
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Mp3File mp3File = data.get(position);
        //观察convertView随ListView滚动情况
        if(convertView==null)
            convertView = mInflater.inflate(R.layout.item_mp3, null);

        convertView.setTag(mp3File);

        BootstrapBadge process_badge = (BootstrapBadge) convertView.findViewById(R.id.process_badge);
        process_badge.setVisibility(View.GONE);

        AwesomeTextView icon_play = (AwesomeTextView) convertView.findViewById(R.id.icon_play);
        //共有四种状态
        //可播放，正在播放，下大下载，未下载  fa-download  fa-save
        if(mp3File.state == Mp3File.LocaleMp3State.Downloaded){
            icon_play.setFontAwesomeIcon("fa_play");
            convertView.setBackgroundResource(R.color.bootstrap_gray_lightest);
        } else if(mp3File.state == Mp3File.LocaleMp3State.Downloading){
            icon_play.setFontAwesomeIcon("fa-save");
            if(process>0 && mp3File.fileName.equals(processFileName)){
                process_badge.setVisibility(View.VISIBLE);
                process_badge.setBadgeText(TW2CN.getInstance(context).toLocal("下載進度")+process);
            }
            convertView.setBackgroundResource(R.color.bootstrap_gray_lightest);
        } else if(mp3File.state == Mp3File.LocaleMp3State.NoDownload){
            icon_play.setFontAwesomeIcon("fa-download");
            convertView.setBackgroundResource(R.color.bootstrap_gray_lightest);
        } else if(mp3File.state == Mp3File.LocaleMp3State.Playing){
            icon_play.setFontAwesomeIcon("fa-music");
            convertView.setBackgroundResource(R.color.bootstrap_brand_warning);
        } else if(mp3File.state == Mp3File.LocaleMp3State.RemoteFileNoexist) {
            icon_play.setFontAwesomeIcon("fa-times");
            convertView.setBackgroundResource(R.color.bootstrap_brand_danger);
        }

        TextView txt = (TextView) convertView.findViewById(R.id.txt);
        txt.setText(TW2CN.getInstance(context).toLocal(mp3File.fileName));

        return convertView;
    }

    public void setProcess(long process, String fileName) {
        this.process = process;
        this.processFileName = fileName;
    }
}
