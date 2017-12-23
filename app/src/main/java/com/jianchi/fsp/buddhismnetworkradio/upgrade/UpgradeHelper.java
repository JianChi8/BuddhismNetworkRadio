package com.jianchi.fsp.buddhismnetworkradio.upgrade;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

/**
 * Desc:
 * Created by ${junhua.li} on 2016/10/21 18:21.
 * Email: lijunhuayc@sina.com
 */
public class UpgradeHelper {
    private static final String TAG = UpgradeHelper.class.getSimpleName();
    private Context mContext;
    private UpgradeConfig config;

    private UpgradeHelper(Builder builder) {
        this.mContext = builder.getContext();
        this.config = builder.getConfig();
        LocalAppInfo.init(mContext);
        MyToast.init(mContext);
        SharedPreferencesUtils.init(mContext);
    }

    public void check() {
        if (isNetworkConnected(mContext)) {
            new CheckAsyncTask(mContext).setConfig(config).execute();
        }
    }

    /**
     * 检测网络是否可用
     * @return
     */
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    public static class Builder {
        private static final String TAG = Builder.class.getSimpleName();
        private Context mContext;
        private UpgradeConfig config;

        public Builder(Context mContext) {
            this.mContext = mContext.getApplicationContext();
            config = new UpgradeConfig();
        }

        Context getContext() {
            return mContext;
        }

        UpgradeConfig getConfig() {
            return config;
        }

        public Builder setUpgradeUrl(String upgradeUrl) {
            if (TextUtils.isEmpty(upgradeUrl)) {
                throw new IllegalArgumentException("The URL is invalid.");
            }
            this.config.setUpgradeUrl(upgradeUrl);
            return this;
        }

        private Builder setAutoStartInstall(boolean autoStartInstall) {
            this.config.setAutoStartInstall(autoStartInstall);
            return this;
        }

        public Builder setQuietDownload(boolean quietDownload) {
            this.config.setQuietDownload(quietDownload);
            return this;
        }

        public Builder setCheckPackageName(boolean checkPackageName) {
            this.config.setCheckPackageName(checkPackageName);
            return this;
        }

        public Builder setIsAboutChecking(boolean aboutChecking) {
            this.config.setAboutChecking(aboutChecking);
            return this;
        }

        public Builder setDelay(long delay) {
            this.config.setDelay(delay);
            return this;
        }

        public UpgradeHelper build() {
            return new UpgradeHelper(this);
        }

    }
}
