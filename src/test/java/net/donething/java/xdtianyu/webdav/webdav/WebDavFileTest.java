package net.donething.java.xdtianyu.webdav.webdav;

import net.donething.java.xdtianyu.webdav.http.HttpAuth;
import net.donething.java.xdtianyu.webdav.http.OkHttp;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;

/**
 * Created by Donething on 2018/02/04
 */

public class WebDavFileTest {

    @Before
    public void setUp() throws Exception {
        HttpAuth.addAuth("https://dav.jianguoyun.com/dav,", "donething@foxmail.com", "ajqx3p288w97q3cz");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void download() throws Exception {
        WebDavFile file = new WebDavFile("https://dav.jianguoyun.com/dav/PrivBackup/qq.txt");
        file.download("/home/zl/temp/qq.txt", false);
    }

    @Test
    public void upload() throws Exception {
        System.out.println(new MimetypesFileTypeMap().getContentType(new File("/home/zl/temp/qq.txt")));
        WebDavFile file = new WebDavFile("https://dav.jianguoyun.com/dav/PrivBackup/qq.txt");
        file.upload("/home/zl/下载/qq.txt");
    }

    @Test
    public void doTest() {
        System.setProperty(OkHttp.LOG_LEVEL_TAG, "NONE");
        System.out.println(System.getProperty(OkHttp.LOG_LEVEL_TAG));
        System.out.println(HttpLoggingInterceptor.Level.valueOf("Test"));
    }
}