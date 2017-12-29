package com.jianchi.fsp.buddhismnetworkradio.upgrade;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.app.AlertDialog;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.jianchi.fsp.buddhismnetworkradio.R;
import com.jianchi.fsp.buddhismnetworkradio.tools.TW2CN;

public class EasyDialog {
    Context mContext;
    UpgradeInfoModel upgradeInfoModel;
    UpgradeConfig config;
    View dialogView;
    TextView tv_versionName;
    TextView tv_fileSize;
    TextView tv_upgradeNotes;
    CheckBox cb_ignoreThisVersion;
    AlertDialog dialog;
    TW2CN tw2CN;

    public EasyDialog(UpgradeInfoModel upgradeInfoModel, UpgradeConfig config, Context mContext){
        this.upgradeInfoModel = upgradeInfoModel;
        this.config = config;
        this.mContext = mContext;

        tw2CN = TW2CN.getInstance(mContext);

        dialogView = LayoutInflater.from(mContext).inflate(R.layout.upgrade_dialog,null);
        tv_versionName = (TextView) dialogView.findViewById(R.id.tv_versionName);
        tv_fileSize = (TextView) dialogView.findViewById(R.id.tv_fileSize);
        tv_upgradeNotes = (TextView) dialogView.findViewById(R.id.tv_upgradeNotes);
        cb_ignoreThisVersion = (CheckBox) dialogView.findViewById(R.id.cb_ignoreThisVersion);
        tv_versionName.setText(tw2CN.toLocal("最新版本:"+upgradeInfoModel.getVersionName()));
        String fileSize = String.format("%.2f", upgradeInfoModel.getFileSize()/1024/1024.0);
        tv_fileSize.setText(tw2CN.toLocal("文件大小:"+fileSize));
        tv_upgradeNotes.setText(tw2CN.toLocal(upgradeInfoModel.getUpgradeNotes()));
    }

    public void show() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext)
                .setTitle(tw2CN.toLocal("有新版本"))
                .setView(dialogView)
                //.setMessage(upgradeInfoModel.getUpgradeNotes())
                .setCancelable(false);

        alertDialog.setPositiveButton(tw2CN.toLocal("立即更新"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startDownload(upgradeInfoModel);
                dialog.dismiss();
            }
        }).setNegativeButton(tw2CN.toLocal("稍后再说"), null);

        dialog = alertDialog.create();
        dialog.show();

        cb_ignoreThisVersion.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SharedPreferencesUtils.setInt("ignoreVersionCode", upgradeInfoModel.getVersionCode());
                    dialog.dismiss();
                }
            }
        });
    }

    private void startDownload(UpgradeInfoModel upgradeInfoModel) {
        String url = "http://amtb.sfzd5.com/";
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(url);
        intent.setData(content_url);
        mContext.startActivity(intent);
    }
}
