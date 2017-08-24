package com.jianchi.fsp.buddhismnetworkradio.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.AwesomeTextView;
import com.jianchi.fsp.buddhismnetworkradio.R;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3Program;
import com.jianchi.fsp.buddhismnetworkradio.tools.TW2CN;

/**
 * Created by fsp on 16-7-6.
 */
public class Mp3ListAdapter extends BaseAdapter {
    String[] data;
    public Mp3Program curMp3;
    private LayoutInflater mInflater;
    Context context;
    public Mp3ListAdapter(Context context, String[] data, Mp3Program curMp3){
        this.context=context;
        this.data=data;
        this.curMp3 = curMp3;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return data.length;
    }

    @Override
    public Object getItem(int position) {
        return data[position];
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
        String holder = data[position];
        //观察convertView随ListView滚动情况
        if(convertView==null)
            convertView = mInflater.inflate(R.layout.item_mp3, null);

        convertView.setTag(holder);

        AwesomeTextView icon_play = (AwesomeTextView) convertView.findViewById(R.id.icon_play);
        if(holder.equals(curMp3.curPlayFile)){
            icon_play.setFontAwesomeIcon("fa-music");
            convertView.setBackgroundResource(R.color.bootstrap_brand_warning);
        } else {
            icon_play.setFontAwesomeIcon("fa_play");
            convertView.setBackgroundResource(R.color.bootstrap_gray_lightest);
        }

        TextView txt = (TextView) convertView.findViewById(R.id.txt);
        txt.setText(TW2CN.getInstance(context).toLocalString(holder));

        return convertView;
    }
}
