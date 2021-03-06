package com.jianchi.fsp.buddhismnetworkradio.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.jianchi.fsp.buddhismnetworkradio.R;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3Channel;
import com.jianchi.fsp.buddhismnetworkradio.mp3.Mp3Program;
import com.jianchi.fsp.buddhismnetworkradio.tools.FileUtils;
import com.jianchi.fsp.buddhismnetworkradio.tools.TW2CN;

import java.util.List;

/**
 * Created by fsp on 16-7-6.
 */
public class Mp3ManagerAdapter extends BaseExpandableListAdapter {
    List<Mp3Program> data;
    List<Mp3Channel> mp3Channels;
    private LayoutInflater mInflater;
    Context context;
    public Mp3ManagerAdapter(Context context, List<Mp3Program> data){
        this.context=context;
        this.data=data;
        this.mInflater = LayoutInflater.from(context);
        String json = FileUtils.readRawAllText(context, R.raw.mp3programs);
        mp3Channels = JSON.parseArray(json, Mp3Channel.class);
    }


    @Override
    public int getGroupCount() {
        return mp3Channels.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mp3Channels.get(groupPosition).items.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mp3Channels.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mp3Channels.get(groupPosition).items.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }


    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.group_channel, null);
        }
        TextView txt = (TextView) convertView.findViewById(R.id.txt);
        txt.setText(TW2CN.getInstance(context).toLocal(mp3Channels.get(groupPosition).category));
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Mp3Program holder = mp3Channels.get(groupPosition).items.get(childPosition);
        //观察convertView随ListView滚动情况
        convertView = mInflater.inflate(R.layout.item_mp3_program_manager, null);

        convertView.setTag(holder);

        TextView txt = (TextView) convertView.findViewById(R.id.txt);
        txt.setText(TW2CN.getInstance(context).toLocal(holder.name));

        TextView info = (TextView) convertView.findViewById(R.id.info);
        info.setText(TW2CN.getInstance(context).toLocal(holder.info));

        CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);

        if(isChecked(holder)){
            checkBox.setChecked(true);
            convertView.setBackgroundResource(R.color.bootstrap_brand_warning);
        } else {
            checkBox.setChecked(false);
            convertView.setBackgroundResource(R.color.bootstrap_gray_lightest);
        }

        return convertView;
    }

    private boolean isChecked(Mp3Program mp3Program){
        for(Mp3Program mp : data){
            if(mp.id.equals(mp3Program.id))
                return true;
        }
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


}
