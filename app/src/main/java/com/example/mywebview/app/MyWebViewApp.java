package com.example.mywebview.app;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.example.mywebview.web.WebFlag;
import com.tencent.smtt.export.external.TbsCoreSettings;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.TbsListener;

import java.util.HashMap;

public class MyWebViewApp extends Application {

    private final static String TAG = MyWebViewApp.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(new Runnable() {
            @Override
            public void run() {
                initThirdService();
            }
        }).start();
    }

    /**
     * 初始化第三方服务，目前就腾讯X5内核
     */
    void initThirdService() {
        QbSdk.setDownloadWithoutWifi(true);
        HashMap<String, Object> map = new HashMap<>();
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER, true);
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE, true);
        QbSdk.initTbsSettings(map);

        QbSdk.setTbsListener(new TbsListener() {


            @Override
            public void onDownloadFinish(int i) {
                Log.i(TAG,"onDownloadFinish");
            }

            @Override
            public void onInstallFinish(int i) {
                Log.i(TAG,"onInstallFinish");
            }

            @Override
            public void onDownloadProgress(int i) {
                Log.i(TAG,"onDownloadProgress:" + i);
            }
        });


        QbSdk.initX5Environment(this, new QbSdk.PreInitCallback() {
            @Override
            public void onCoreInitFinished() {
                Log.i(TAG, "QbSdk onCoreInitFinished");
            }

            @Override
            public void onViewInitFinished(boolean initX5) {
                Toast.makeText(MyWebViewApp.this, "X5 init result:"+initX5, Toast.LENGTH_SHORT).show();
                WebFlag.setQbdWebviewInit(initX5);
                Log.i(TAG, "QbSdk onViewInitFinished:"+initX5);
            }
        });

    }
}
