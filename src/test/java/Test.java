import net.donething.java.xdtianyu.webdav.http.HttpAuth;
import net.donething.java.xdtianyu.webdav.webdav.WebDavFile;
import okhttp3.MediaType;

/**
 * Created by Donething on 2018/02/03
 */

public class Test {
    public static void main(String[] args) {
        System.out.println("媒体类型：" + MediaType.parse(""));
        HttpAuth.addAuth("https://dav.jianguoyun.com/dav,", "donething@foxmail.com", "ajqx3p288w97q3cz");
        try {
            WebDavFile file = new WebDavFile("https://dav.jianguoyun.com/dav/PrivBackup/测试文件夹22");
            file.makeAsDir();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
