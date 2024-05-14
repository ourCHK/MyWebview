package com.example.mywebview;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSession.NavigationDelegate;
import org.mozilla.geckoview.GeckoSession.ProgressDelegate;
import org.mozilla.geckoview.GeckoSession.ScrollDelegate;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;

public class MainActivity extends AppCompatActivity {

    public final static int PERMISSION_CODE = 1;

    public final static String IMAGE_CACHE_PATH = "ImageCache";

    public final static String IMAGE_CACHE_NAME = "imageCache.json";

    private final static String IMAGE_PLACE_HOLDER_NAME = "image_place_holder.png";

    WebView webview;

    com.tencent.smtt.sdk.WebView qbWebview;

    GeckoView geckoView;

    EditText input;

    TextView progress;
    TextView verticalProgress;

    TextView refresh;
    TextView clearUrl;
    TextView enter;
    TextView setUa;
    TextView uaFlag;

    TextView webType;

    TextView changeWebType;
    TextView webTypFlag;
    TextView back;
    TextView forward;

    TextView remoteState;
    TextView remoteRefresh;

    TextView stickTop;

    GeckoSession session = new GeckoSession();
    GeckoRuntime runtime;


    int pageHeight = 1;

    /**
     1代表webview,2代表geckoView
     *
     */
    int webFlag = 1;

    boolean initGeckoView = false;

    boolean initQbsWebview = false;

    int REFRESH_HEIGHT = 0x01;

    int REFRESH_CANCEL = 0x02;

    /**
     * 刷新流量
     */
    int REFRESH_TRAFFIC = 0x03;

    Handler handler;

    OkHttpClient okHttpClient = new OkHttpClient();

    HashMap<String,String> mCacheMap = new HashMap<>();

    //存放拦截图片域名的列表
    List<String> imageDomainList;

    //存放图片拦截关键字的列表
    List<String> imageKeywordList;

    long startTraffic = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        requestIP();
//        initView();
        dataInit();
        checkStoragePermission();
    }

    void dataInit() {
        readImageCache();
        readConfigInterceptImageDomain();
        readConfigInterceptImageKeyword();
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == REFRESH_HEIGHT) {
                    if (webFlag == 2) {
                        installBuilderHeight();
                    }
                } else if (msg.what == REFRESH_CANCEL) {
                    handler.removeMessages(REFRESH_HEIGHT);
                } else if (msg.what == REFRESH_TRAFFIC) {
                    startTraffics();
                }
            }
        };
        startTraffics();
    }


    void startTraffics() {
        if (startTraffic == 0) {
            startTraffic = TrafficStats.getUidRxBytes(Process.myUid()) + TrafficStats.getUidTxBytes(Process.myUid());
        }
        long usage = TrafficStats.getUidRxBytes(Process.myUid()) + TrafficStats.getUidTxBytes(Process.myUid()) - startTraffic;
        Log.i(MainActivity.class.getSimpleName(), "usage:"+(usage/1024/8));
        handler.sendEmptyMessageDelayed(REFRESH_TRAFFIC, 3000);
    }


    @SuppressLint({"SetJavaScriptEnabled", "SetTextI18n"})
    void initView() {
        progress = findViewById(R.id.progress);
        geckoView = findViewById(R.id.geckoView);
        qbWebview = findViewById(R.id.qbsWebview);
        webview = findViewById(R.id.webview);
        initWebview();
        verticalProgress = findViewById(R.id.verticalProgress);
        input = findViewById(R.id.input);

        refresh = findViewById(R.id.refresh);
        clearUrl = findViewById(R.id.clearUrl);
        back = findViewById(R.id.back);
        forward = findViewById(R.id.forward);
        enter = findViewById(R.id.enter);
        setUa = findViewById(R.id.setUa);
        uaFlag = findViewById(R.id.uaFlag);
        webType = findViewById(R.id.webType);
        changeWebType = findViewById(R.id.changeWebType);
        webTypFlag = findViewById(R.id.webTypeFlag);

        remoteRefresh = findViewById(R.id.refreshRemote);
        remoteState = findViewById(R.id.remoteState);

        stickTop = findViewById(R.id.stickTop);
        input.setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                return source.toString().replace("\n", "");
            }
        }});

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webFlag == 1) {
                    webview.goBack();
                } else if (webFlag == 2){
                    session.goBack();
                } else if (webFlag == 3) {
                    qbWebview.goBack();
                }
            }
        });

        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webFlag == 1) {
                    webview.goForward();
                } else if (webFlag == 2){
                    session.goForward();
                } else if (webFlag == 3) {
                    qbWebview.goForward();
                }
            }
        });

        //查询按钮
        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (input.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "输入内容不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                hideKeyboard(input);
                if (webFlag == 1) {
                    webview.loadUrl(input.getText().toString());
                } else if (webFlag == 2) {
                    session.loadUri(input.getText().toString());
                } else if (webFlag == 3) {
                    qbWebview.loadUrl(input.getText().toString());
                }
            }
        });
        //设置ua按钮
        setUa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uaFlag.setText("");
                String ua = getUa();
                if (ua == null || ua.isEmpty()) {
                    uaFlag.setText("2");
                    Toast.makeText(MainActivity.this, "Ua设置失败", Toast.LENGTH_SHORT).show();
                } else {
                    if (webFlag == 1) {
                        webview.getSettings().setUserAgentString(ua);
                    } else if (webFlag == 2){
                        session.getSettings().setUserAgentOverride(ua);
                    } else if (webFlag == 3) {
                        qbWebview.getSettings().setUserAgent(ua);
                    }
                    Toast.makeText(MainActivity.this, "Ua设置成功", Toast.LENGTH_SHORT).show();
                    uaFlag.setText("1");
                }
            }
        });

        changeWebType.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                webTypFlag.setText("");
                changeWebType();
            }
        });

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webFlag == 1) {
                    webview.reload();
                } else if (webFlag == 2) {
                    session.reload();
                } else if (webFlag == 3) {
                    qbWebview.reload();
                }
            }
        });



        clearUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input.setText("");
            }
        });


        remoteRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestIP();
            }
        });

        stickTop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webFlag == 1) {
                    webview.scrollTo(0,0);
                } else if (webFlag == 2) {
                    geckoView.getPanZoomController().scrollToTop();
                } else if (webFlag == 3) {
                    qbWebview.flingScroll(0,0);
                }
            }
        });

    }


    void initWebview() {
        WebSettings settings = webview.getSettings();
        // 如果访问的页面中有JavaScript，则WebView必须设置支持JavaScript，否则显示空白页面
        settings.setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        settings.setAllowContentAccess(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        webview.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                input.setText(url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {

                String url = request.getUrl().toString();
                if (!checkInterceptImageDomain(url)) {
                    return super.shouldInterceptRequest(view,request);
                }

                String md5Key = calculateMD5(url);
                if (md5Key == null) {
                    return super.shouldInterceptRequest(view,request);
                }
                String filePath = getImageCachePath(md5Key);
                if (filePath != null) {
                    File file = new File(getExternalCacheDir(),IMAGE_CACHE_PATH+File.separator+md5Key);
                    //说明命中了缓存
                    WebResourceResponse webResourceResponse = null;
                    try {
                        String imagePlaceHolderPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+ IMAGE_PLACE_HOLDER_NAME;
                        webResourceResponse = new WebResourceResponse("image/*","utf-8",new FileInputStream(imagePlaceHolderPath));
                        Log.i("MainActivity Cache","hit "+url);
                        return webResourceResponse;
                    } catch (FileNotFoundException e) {
                        file.delete();
                    }
                }
                Headers headers = Headers.of(request.getRequestHeaders());

                Request customRequest = new Request.Builder().url(url).headers(headers).build();
                Call call = okHttpClient.newCall(customRequest);
                try {
                    Response customResponse = call.execute();
                    if (customResponse.isSuccessful()) {
                        String contentType = customResponse.header("Content-Type","");
                        //如果是图片，我们给缓存起来
                        if (contentType.contains("image/")) {
                            byte[] buffer = new byte[1024];
                            int len = 0;
                            InputStream is = customResponse.body().byteStream();
                            File file = new File(getExternalCacheDir(),IMAGE_CACHE_PATH+File.separator+md5Key);
                            if (file.exists()) {
                                file.delete();
                            }
                            //保存图片文件
                            file.createNewFile();
                            FileOutputStream os = new FileOutputStream(file);
                            while ((len = is.read(buffer)) > 0) {
                                os.write(buffer,0,len);
                            }
                            os.flush();
                            os.close();
                            is.close();
                            Log.i("MainActivity Cache","success "+url);
                            putImageCachePath(md5Key);
                            String imagePlaceHolderPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+ IMAGE_PLACE_HOLDER_NAME;
                            WebResourceResponse webResourceResponse = new WebResourceResponse(contentType,"",new FileInputStream(imagePlaceHolderPath));
                            Map<String,String> headerMap = new HashMap<>();
                            Set<String> headerNames = customResponse.headers().names();

                            for (int i=0; i<headerNames.size(); i++) {
                                headerMap.put(customResponse.headers().name(i),customResponse.headers().value(i));
                            }
                            webResourceResponse.setResponseHeaders(headerMap);
                            return webResourceResponse;
                        } else {
                            WebResourceResponse webResourceResponse = new WebResourceResponse(contentType,"",customResponse.body().byteStream());
                            Map<String,String> headerMap = new HashMap<>();
                            Set<String> headerNames = customResponse.headers().names();

                            for (int i=0; i<headerNames.size(); i++) {
                                headerMap.put(customResponse.headers().name(i),customResponse.headers().value(i));
                            }
                            webResourceResponse.setResponseHeaders(headerMap);
                            return webResourceResponse;
                        }
                    }
                } catch (IOException e) {
                    Log.i("MainActivity Cache","fail "+url);
                    e.printStackTrace();
                    return super.shouldInterceptRequest(view, request);
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        webview.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                float contentHeight = webview.getContentHeight() * webview.getScale();
                float webviewHeight = webview.getHeight();
                float verticalProgress = scrollY / (contentHeight-webviewHeight);
                MainActivity.this.verticalProgress.setText(String.valueOf((int)(round(verticalProgress,2)* 100)));
            }
        });

        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progress.setText(String.valueOf(newProgress));
            }
        });
    }

    void initQbsWebview() {
        if (initQbsWebview) {
            //防止多次初始化
            return;
        }
        initQbsWebview = true;
        com.tencent.smtt.sdk.WebSettings settings = qbWebview.getSettings();
        // 如果访问的页面中有JavaScript，则WebView必须设置支持JavaScript，否则显示空白页面
        settings.setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        settings.setAllowContentAccess(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        qbWebview.setWebViewClient(new com.tencent.smtt.sdk.WebViewClient() {
            @Override
            public void onPageStarted(com.tencent.smtt.sdk.WebView webView, String url, Bitmap bitmap) {
                super.onPageStarted(webView, url, bitmap);
                input.setText(url);

            }

            @Override
            public com.tencent.smtt.export.external.interfaces.WebResourceResponse shouldInterceptRequest(com.tencent.smtt.sdk.WebView view, com.tencent.smtt.export.external.interfaces.WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (!checkInterceptImageDomain(url)) {
                    return super.shouldInterceptRequest(view,request);
                }

                String md5Key = calculateMD5(url);
                if (md5Key == null) {
                    return super.shouldInterceptRequest(view,request);
                }
                String filePath = getImageCachePath(md5Key);
                if (filePath != null) {
                    File file = new File(getExternalCacheDir(),IMAGE_CACHE_PATH+File.separator+md5Key);
                    //说明命中了缓存
                    com.tencent.smtt.export.external.interfaces.WebResourceResponse webResourceResponse = null;
                    try {
                        String imagePlaceHolderPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+ IMAGE_PLACE_HOLDER_NAME;
                        webResourceResponse = new com.tencent.smtt.export.external.interfaces.WebResourceResponse("image/*","utf-8",new FileInputStream(imagePlaceHolderPath));
                        Log.i("MainActivity Cache","hit "+url);
                        return webResourceResponse;
                    } catch (FileNotFoundException e) {
                        file.delete();
                    }
                }
                Headers headers = Headers.of(request.getRequestHeaders());

                Request customRequest = new Request.Builder().url(url).headers(headers).build();
                Call call = okHttpClient.newCall(customRequest);
                try {
                    Response customResponse = call.execute();
                    if (customResponse.isSuccessful()) {
                        String contentType = customResponse.header("Content-Type","");
                        //如果是图片，我们给缓存起来
                        if (contentType.contains("image/")) {
                            byte[] buffer = new byte[1024];
                            int len = 0;
                            InputStream is = customResponse.body().byteStream();
                            File file = new File(getExternalCacheDir(),IMAGE_CACHE_PATH+File.separator+md5Key);
                            if (file.exists()) {
                                file.delete();
                            }
                            //保存图片文件
                            file.createNewFile();
                            FileOutputStream os = new FileOutputStream(file);
                            while ((len = is.read(buffer)) > 0) {
                                os.write(buffer,0,len);
                            }
                            os.flush();
                            os.close();
                            is.close();
                            Log.i("MainActivity Cache","success "+url);
                            putImageCachePath(md5Key);
                            String imagePlaceHolderPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+ IMAGE_PLACE_HOLDER_NAME;
                            com.tencent.smtt.export.external.interfaces.WebResourceResponse webResourceResponse = new com.tencent.smtt.export.external.interfaces.WebResourceResponse(contentType,"",new FileInputStream(imagePlaceHolderPath));
                            Map<String,String> headerMap = new HashMap<>();
                            Set<String> headerNames = customResponse.headers().names();

                            for (int i=0; i<headerNames.size(); i++) {
                                headerMap.put(customResponse.headers().name(i),customResponse.headers().value(i));
                            }
                            webResourceResponse.setResponseHeaders(headerMap);
                            return webResourceResponse;
                        } else {
                            com.tencent.smtt.export.external.interfaces.WebResourceResponse webResourceResponse = new com.tencent.smtt.export.external.interfaces.WebResourceResponse(contentType,"",customResponse.body().byteStream());
                            Map<String,String> headerMap = new HashMap<>();
                            Set<String> headerNames = customResponse.headers().names();

                            for (int i=0; i<headerNames.size(); i++) {
                                headerMap.put(customResponse.headers().name(i),customResponse.headers().value(i));
                            }
                            webResourceResponse.setResponseHeaders(headerMap);
                            return webResourceResponse;
                        }
                    }
                } catch (IOException e) {
                    Log.i("MainActivity Cache","fail "+url);
                    e.printStackTrace();
                    return super.shouldInterceptRequest(view, request);
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        qbWebview.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                float contentHeight = qbWebview.getContentHeight() * qbWebview.getScale();
                float webviewHeight = qbWebview.getHeight();
                float verticalProgress = scrollY / (contentHeight-webviewHeight);
                MainActivity.this.verticalProgress.setText(String.valueOf((int)(round(verticalProgress,2)* 100)));
            }
        });

        qbWebview.setWebChromeClient(new com.tencent.smtt.sdk.WebChromeClient() {
            @Override
            public void onProgressChanged(com.tencent.smtt.sdk.WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progress.setText(String.valueOf(newProgress));
            }
        });

    }

    public String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(),0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return "";
    }

    public static String getWiFiIPAddress(Context context) {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr != null && wifiMgr.getConnectionInfo() != null) {
            int ipAddress = wifiMgr.getConnectionInfo().getIpAddress();
            return Formatter.formatIpAddress(ipAddress);
        }
        return null;
    }

    public void hideKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            // 假设当前获得焦点的View是editText
            View view = editText; // 这里的editText是你想要关闭键盘的视图
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static double round(double value, int places) {
        if (places < 0)  {
            throw new IllegalArgumentException();
        }
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }


    public void requestIP() {
        remoteState.setText("");
        OkHttpClient okHttpClient = new OkHttpClient();
        final Request request = new Request.Builder().url("http://lumtest.com/myip.json").get().build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        remoteState.setText("2");
                        Toast.makeText(MainActivity.this, "lumtest request fail!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                writeIpJson(json);
                Gson gson = new Gson();
                RemoteData data = gson.fromJson(json,RemoteData.class);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        remoteState.setText("1");
                    }
                });
            }
        });

    }

    void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},PERMISSION_CODE);
        } else {
            initView();
            requestIP();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CODE: {
                // 如果请求被取消，则结果数组为空。
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    initView();
                    requestIP();
                } else {
                    finish();
                }
            }
            default:
                break;
        }
    }

    public String getUa() {
        return readFileString(Environment.getExternalStorageDirectory().getAbsolutePath()+"/ua.txt");
    }

    void initGeckoView() {
        if (initGeckoView) {
            //防止多次初始化
            return;
        }
        initGeckoView = true;
        runtime = GeckoRuntime.create(this);
        session.setNavigationDelegate(new NavigationDelegate() {

            @Override
            public void onLocationChange(@NonNull GeckoSession session, @Nullable String url, @NonNull List<GeckoSession.PermissionDelegate.ContentPermission> perms, @NonNull Boolean hasUserGesture) {
                NavigationDelegate.super.onLocationChange(session, url, perms, hasUserGesture);
                input.setText(url);
                //移除之前的message
                handler.removeMessages(REFRESH_HEIGHT);
                handler.sendEmptyMessageDelayed(REFRESH_HEIGHT,500);
            }

        });
        session.setProgressDelegate(new ProgressDelegate() {
            @Override
            public void onProgressChange(@NonNull GeckoSession session, int progress) {
                ProgressDelegate.super.onProgressChange(session, progress);
                MainActivity.this.progress.setText(String.valueOf(progress));
            }

            @Override
            public void onPageStop(@NonNull GeckoSession session, boolean success) {
                ProgressDelegate.super.onPageStop(session, success);
                verticalProgress.setText(String.valueOf(0));
            }
        });


        session.setScrollDelegate(new ScrollDelegate() {
            @Override
            public void onScrollChanged(@NonNull GeckoSession session, int scrollX, int scrollY) {
                ScrollDelegate.super.onScrollChanged(session, scrollX, scrollY);
                RectF rectF = new RectF();
                session.getClientBounds(rectF);
                Log.i("MainActivity", "scrollY:"+scrollY+" "+rectF.height()+" "+runtime.getSettings().getDisplayDpiOverride());

                float progress = scrollY / (pageHeight-rectF.height());
                if (progress > 1) {
                    progress = 1;
                }
                verticalProgress.setText(String.valueOf((int)(round(progress,2)* 100)));
            }
        });
        session.open(runtime);
        geckoView.setSession(session);
        GeckoSessionSettings settings = geckoView.getSession().getSettings();
        settings.setAllowJavascript(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveImageCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeMessages(REFRESH_HEIGHT);
        if (runtime != null) {
            runtime.shutdown();
        }
    }

    void changeWebType() {
        if (webFlag == 1) {
            //说明当前是webview,需要切换到GeckoView
            webFlag = 2;
            webType.setText(String.valueOf(webFlag));

            geckoView.setVisibility(View.VISIBLE);
            webview.setVisibility(View.GONE);
            qbWebview.setVisibility(View.GONE);
            initGeckoView();
        } else if (webFlag == 2){
            //说明是当前是GeckoView
            webFlag = 3;
            webType.setText(String.valueOf(webFlag));

            qbWebview.setVisibility(View.VISIBLE);
            webview.setVisibility(View.GONE);
            geckoView.setVisibility(View.GONE);
            initQbsWebview();
        } else if (webFlag == 3) {
            //说明是当前是QbsWebview
            webFlag = 1;
            webType.setText(String.valueOf(webFlag));

            webview.setVisibility(View.VISIBLE);
            qbWebview.setVisibility(View.GONE);
            geckoView.setVisibility(View.GONE);
        }
        webTypFlag.setText("1");
    }

    void writeIpJson(String json) {
        File file = new File(Environment.getExternalStorageDirectory(),"ip.json");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStream outputStream = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            BufferedWriter bw = new BufferedWriter(writer);
            bw.write(json);
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void installBuilderHeight() {
        runtime.getWebExtensionController().installBuiltIn("resource://android/assets/messaging/")
                .accept(new GeckoResult.Consumer<WebExtension>() {
                    @Override
                    public void accept(@Nullable WebExtension webExtension) {
                        if (webExtension != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    session.getWebExtensionController().setMessageDelegate(webExtension, new WebExtension.MessageDelegate() {
                                        @Nullable
                                        @Override
                                        public GeckoResult<Object> onMessage(@NonNull String nativeApp, @NonNull Object message, @NonNull WebExtension.MessageSender sender) {
                                            Log.i("MainActivity Height", message.toString());
                                            pageHeight = Integer.parseInt(message.toString());
                                            handler.sendEmptyMessageDelayed(REFRESH_HEIGHT, 1000);
                                            return WebExtension.MessageDelegate.super.onMessage(nativeApp, message, sender);
                                        }

                                        @Nullable
                                        @Override
                                        public void onConnect(@NonNull WebExtension.Port port) {
                                            Log.i("MainActivity", "onConnect");
                                            WebExtension.MessageDelegate.super.onConnect(port);
                                        }
                                    }, "browser");
                                }
                            });
                        }
                    }
                }, new GeckoResult.Consumer<Throwable>() {
                    @Override
                    public void accept(@Nullable Throwable throwable) {
                        Log.e("MainActivity", "error register", throwable);
                    }
                });
    }

    public static String calculateMD5(String input) {
        try {
            // 创建MD5消息摘要对象
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 将输入字符串转换为字节数组
            byte[] inputBytes = input.getBytes();
            // 计算MD5哈希值
            byte[] hashBytes = md.digest(inputBytes);

            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            // 返回十六进制字符串作为MD5哈希值
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    synchronized String getImageCachePath(String key) {
        return mCacheMap.get(key);
    }

    synchronized void putImageCachePath(String key) {
        mCacheMap.put(key, key);
    }

    void readImageCache() {
        //创建目录
        File fileDir = new File(getExternalCacheDir(),IMAGE_CACHE_PATH);
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        File file = new File(getExternalCacheDir(),IMAGE_CACHE_PATH+File.separator+IMAGE_CACHE_NAME);
        if (file.exists()) {
            try {
                Gson gson = new Gson();
                InputStream inputStream = new FileInputStream(file);
                InputStreamReader reader = new InputStreamReader(inputStream);
                BufferedReader bf = new BufferedReader(reader);
                StringBuilder text = new StringBuilder();
                String temp;
                while ((temp = bf.readLine()) != null) {
                    text.append(temp);
                }
                inputStream.close();
                Map<String,String> dataMap = gson.fromJson(text.toString(), new TypeToken<Map<String,String>>(){}.getType());
                mCacheMap.putAll(dataMap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 保存图片缓存的json
     */
    void saveImageCache() {
        Gson gson = new Gson();
        String json = gson.toJson(mCacheMap);
        File file = new File(getExternalCacheDir(),IMAGE_CACHE_PATH+File.separator+IMAGE_CACHE_NAME);
        try {
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            OutputStream outputStream = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            BufferedWriter bw = new BufferedWriter(writer);
            bw.write(json);
            bw.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查是否拦截url
     * @param url
     * @return
     */
    boolean checkInterceptImageDomain(String url) {
        if (imageDomainList == null || imageDomainList.isEmpty()) {
            return false;
        }
        for (String imageDomain: imageDomainList) {
            if (url.startsWith(imageDomain) && checkPairImageKeyword(url)) {
                return true;
            }
        }
        return false;
    };

    /**
     * 检查是否匹配关键字
     * @param url
     * @return
     */
    boolean checkPairImageKeyword(String url) {
        if (imageKeywordList == null || imageKeywordList.isEmpty()) {
            return true;
        }
        for (String imageKeyword: imageKeywordList) {
            if (url.contains(imageKeyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 读取配置的拦截域名图片的文件
     */
    void readConfigInterceptImageDomain() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/image_domain.json";
        String configInterceptString = readFileString(path);

        Gson gson = new Gson();
        try {
            imageDomainList = gson.fromJson(configInterceptString,new TypeToken<List<String>>(){}.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取配置的拦截域名图片的关键字
     */
    void readConfigInterceptImageKeyword() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/image_path_keyword.json";
        String configInterceptString = readFileString(path);

        Gson gson = new Gson();
        try {
            imageKeywordList = gson.fromJson(configInterceptString,new TypeToken<List<String>>(){}.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readFileString(String path) {
        File file = new File(path);
        try {
            InputStream inputStream = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(inputStream);
            BufferedReader bf = new BufferedReader(reader);
            StringBuilder text = new StringBuilder();
            String temp;
            while ((temp = bf.readLine()) != null) {
                text.append(temp);
            }
            inputStream.close();
            return text.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}