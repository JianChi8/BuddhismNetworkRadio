package com.jianchi.fsp.buddhismnetworkradio.upgrade;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * Desc:
 * Created by ${junhua.li} on 2016/10/31 18:29.
 * Email: lijunhuayc@sina.com
 */
public class LocalAppInfo {
    private static final String TAG = "LocalAppInfo";
    private String appName;
    private String packageName;
    private String versionName;
    private int versionCode;

    public int getIgnoreVersionCode() {
        return ignoreVersionCode;
    }

    public void setIgnoreVersionCode(int ignoreVersionCode) {
        this.ignoreVersionCode = ignoreVersionCode;
    }

    private int ignoreVersionCode;

    private static LocalAppInfo localAppInfo;

    public static void init(Context mContext){
        localAppInfo = new LocalAppInfo();
        PackageManager pm = mContext.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (info != null) {
                ApplicationInfo appInfo = info.applicationInfo;
                localAppInfo.setAppName(pm.getApplicationLabel(appInfo).toString());
                localAppInfo.setPackageName(info.packageName);
                localAppInfo.setVersionCode(info.versionCode);
                localAppInfo.setVersionName(info.versionName);
            }

            localAppInfo.ignoreVersionCode = SharedPreferencesUtils.getInt("ignoreVersionCode", 0);

            LogUtil.d(TAG, "about: LocalAppInfo = " + localAppInfo.toString());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static LocalAppInfo getLocalAppInfo() {
        if(null == localAppInfo){
            throw new RuntimeException("LocalAppInfo is not init.");
        }
        return localAppInfo;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    @Override
    public String toString() {
        return "LocalAppInfo{" +
                "appName='" + appName + '\'' +
                ", packageName='" + packageName + '\'' +
                ", versionName='" + versionName + '\'' +
                ", versionCode=" + versionCode +
                '}';
    }
}
