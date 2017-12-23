package com.jianchi.fsp.buddhismnetworkradio.upgrade;

import android.content.Context;
import android.os.AsyncTask;

import com.jianchi.fsp.buddhismnetworkradio.tools.TW2CN;

/**
 * Desc:
 * Created by ${junhua.li} on 2017/02/09 11:45.
 * Email: lijunhuayc@sina.com
 */
class CheckAsyncTask extends AsyncTask<String, Integer, UpgradeInfoResult> {
    private static final String TAG = CheckAsyncTask.class.getSimpleName();
    private UpgradeConfig config;
    private Context mContext;
    TW2CN tw2CN;

    CheckAsyncTask(Context context) {
        this.mContext = context;
        tw2CN = TW2CN.getInstance(context);
    }

    CheckAsyncTask setConfig(UpgradeConfig config) {
        this.config = config;
        return this;
    }

    //在onPreExecute()完成后立即执行，用于执行较为费时的操作
    @Override
    protected UpgradeInfoResult doInBackground(String... params) {
        try {
            if (config.getDelay() > 0) {
                Thread.sleep(config.getDelay());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return HttpUtils.parseResult(HttpUtils.getResponse(config.getUpgradeUrl()));
    }

    //当后台操作结束时，此方法将会被调用，计算结果将做为参数传递到此方法中，直接将结果显示到UI组件上。
    @Override
    protected void onPostExecute(UpgradeInfoResult upgradeInfoResult) {
        super.onPostExecute(upgradeInfoResult);
        if (null != upgradeInfoResult && upgradeInfoResult.getStatus() == 1) {
            if (null != upgradeInfoResult.getData()) {
                executeResult(upgradeInfoResult.getData());
            } else if (config.isAboutChecking()) {
                //if is "about" check upgrade, prompt than it's the latest version.
                MyToast.showToast(tw2CN.toLocalString("已是最新版本"));
            }
        }
    }

    private void executeResult(UpgradeInfoModel upgradeInfoModel) {
        LocalAppInfo localAppInfo = LocalAppInfo.getLocalAppInfo();
        if (config.isCheckPackageName()
                && !upgradeInfoModel.getPackageName().equals(localAppInfo.getPackageName())) {
            //enable set callback notify coder and coder can dispose callback notify server or not.
            MyToast.showToast(tw2CN.toLocalString("软件包不相同"));
            return;
        }
        if (upgradeInfoModel.getVersionCode() > localAppInfo.getVersionCode() && upgradeInfoModel.getVersionCode() != localAppInfo.getIgnoreVersionCode()) {
            //打开对话框
            new EasyDialog(upgradeInfoModel, config, mContext).show();
        } else if (config.isAboutChecking()) {
            //if is "about" check upgrade, prompt than it's the latest version.
            MyToast.showToast(tw2CN.toLocalString("已是最新版本"));
        }
    }

}
