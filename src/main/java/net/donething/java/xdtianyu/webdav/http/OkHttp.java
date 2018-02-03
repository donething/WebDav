package net.donething.java.xdtianyu.webdav.http;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class OkHttp {

    private OkHttpClient okHttpClient;

    private OkHttp() {
    }

    public static OkHttp getInstance() {
        return SingletonHelper.INSTANCE;
    }

    public void setClient(OkHttpClient client) {
        okHttpClient = client;
    }

    public OkHttpClient client() {
        if (okHttpClient == null) {
            String logLevelStr = System.getProperty(LOG_LEVEL_TAG, HttpLoggingInterceptor.Level.BASIC.name());

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.valueOf(logLevelStr));
            okHttpClient = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        }
        return okHttpClient;
    }

    private static class SingletonHelper {
        private final static OkHttp INSTANCE = new OkHttp();
    }

    /**
     * 设置Log显示等级
     */
    public static String LOG_LEVEL_TAG = "log_level";

    /**
     * Log等级
     */
    public static enum LOG_LEVEL {
        NONE,
        BASIC,
        HEADERS,
        BODY
    }
}
