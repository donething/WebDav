import net.donething.java.xdtianyu.webdav.http.HttpAuth;
import net.donething.java.xdtianyu.webdav.webdav.WebDavFile;

import java.lang.reflect.Field;

/**
 * Created by Donething on 2018/02/03
 */

public class Test {
    public static void main(String[] args) {
        HttpAuth.addAuth("https://dav.jianguoyun.com/dav,", "donething@foxmail.com", "ajqx3p288w97q3cz");
        try {
            WebDavFile file = new WebDavFile("https://dav.jianguoyun.com/dav/PrivBackup");
            file.indexFileInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 打印对象内的所有属性
    public static <T> T printAllAttrs(String s1, Object o) {
        try {
            Class<?> c = Class.forName(s1);
            Field[] fields = c.getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);
            }
            System.out.println("=============" + s1 + "===============");
            for (Field f : fields) {
                String field = f.toString().substring(f.toString().lastIndexOf(".") + 1); //取出属性名称
                System.out.println(field + " --> " + f.get(o));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
