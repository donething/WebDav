package net.donething.java.xdtianyu.webdav.webdav;

import net.donething.java.xdtianyu.webdav.http.Handler;
import net.donething.java.xdtianyu.webdav.http.HttpAuth;
import net.donething.java.xdtianyu.webdav.http.OkHttp;
import net.donething.java.xdtianyu.webdav.webdav.model.MultiStatus;
import net.donething.java.xdtianyu.webdav.webdav.model.Prop;
import okhttp3.*;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class WebDavFile {
    public final static String TAG = WebDavFile.class.getSimpleName();
    public final static String OBJECT_NOT_EXISTS_TAG = "ObjectNotFound";
    // 指定返回哪些属性
    private final static String DIR = "<?xml version=\"1.0\"?>\n" +
            "<a:propfind xmlns:a=\"DAV:\">\n" +
            "<a:prop>\n" +
            "<a:displayname/>\n<a:resourcetype/>\n<a:getcontentlength/>\n<a:creationdate/>\n<a:getlastmodified/>\n%s" +
            "</a:prop>\n" +
            "</a:propfind>";

    private URL url;
    private String httpUrl;

    private String canon;
    private long createTime;
    private long lastModified;
    private long size;
    private boolean isDirectory = true;
    private boolean exists = false;
    private String parent = "";
    private String urlName = "";

    private OkHttpClient okHttpClient;

    public WebDavFile(String url) throws MalformedURLException {
        this.url = new URL(null, url, Handler.HANDLER);
        okHttpClient = OkHttp.getInstance().client();
    }

    public String getUrl() {
        if (httpUrl == null) {
            String raw = url.toString().replace("davs://", "https://").replace("dav://", "http://");
            try {
                httpUrl = URLEncoder.encode(raw, "UTF-8")
                        .replaceAll("\\+", "%20")
                        .replaceAll("%3A", ":")
                        .replaceAll("%2F", "/");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return httpUrl;
    }

    public String getPath() {
        return url.toString();
    }

    /**
     * 填充文件信息。实例化WebDAVFile对象时，并没有将远程文件的信息填充到实例中。需要手动填充！
     *
     * @return 远程文件是否存在
     */
    public boolean indexFileInfo() {
        Response response = propFindResponse(new ArrayList<>());
        String s = "";
        try {
            if (response == null || !response.isSuccessful()) {
                this.exists = false;
                return false;
            }
            s = response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Serializer serializer = new Persister();
        try {
            MultiStatus multiStatus = serializer.read(MultiStatus.class, s);
            net.donething.java.xdtianyu.webdav.webdav.model.Response res = multiStatus.getResponse().get(0);
            Prop prop = res.getPropstat().getProp();
            this.canon = prop.getDisplayname();
            this.createTime = 0;
            this.lastModified = 0;
            this.size = prop.getGetcontentlength();
            this.isDirectory = prop.getResourcetype().getCollection() != null;
            this.exists = true;
            // this.parent = "";
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 列出当前路径下的文件。默认列出文件的如下属性：displayname、resourcetype、getcontentlength、creationdate、getlastmodified
     *
     * @return 文件列表
     */
    public WebDavFile[] listFiles() {
        return listFiles(new ArrayList<>());
    }

    /**
     * 列出当前路径下的文件
     *
     * @param propsList 指定列出文件的哪些属性
     * @return 文件列表
     */
    public WebDavFile[] listFiles(ArrayList<String> propsList) {
        Response response = propFindResponse(propsList);
        try {
            if (response.isSuccessful()) {
                return parseDir(response.body().string());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new WebDavFile[0];
    }

    private Response propFindResponse(ArrayList<String> propsList) {
        StringBuilder requestProps = new StringBuilder();
        for (String p : propsList) {
            requestProps.append("<a:" + p + "/>\n");
        }
        String requestPropsStr;
        if (requestProps.toString().isEmpty()) {
            requestPropsStr = DIR.replace("%s", "");
        } else {
            requestPropsStr = String.format(DIR, requestProps.toString() + "\n");
        }
        Request.Builder request = new Request.Builder()
                .url(getUrl())
                // 添加RequestBody对象，可以只返回的属性。如果设为null，则会返回全部属性
                // 注意：尽量手动指定需要返回的属性。若返回全部属性，可能后由于Prop.java里没有该属性名，而崩溃。
                .method("PROPFIND", RequestBody.create(MediaType.parse("text/plain"), requestPropsStr));

        HttpAuth.Auth auth = HttpAuth.getAuth(url.toString());
        if (auth != null) {
            request.header("Authorization", Credentials.basic(auth.getUser(), auth.getPass()));
        }

        try {
            return okHttpClient.newCall(request.build()).execute();
        } catch (Exception e) {
            //catch (IOException | XmlPullParserException | IllegalArgumentException e)
            e.printStackTrace();
        }
        return null;
    }

    private WebDavFile[] parseDir(String s) {
        List<WebDavFile> list = new ArrayList<>();
        Serializer serializer = new Persister();
        try {
            MultiStatus multiStatus = serializer.read(MultiStatus.class, s);
            String parent = url.toString();
            for (net.donething.java.xdtianyu.webdav.webdav.model.Response response : multiStatus.getResponse()) {
                String path = url.getProtocol() + "://" + url.getHost() +
                        (url.getPort() != -1 ? ":" + url.getPort() : "") +
                        URLDecoder.decode(response.getHref().replace("+", "%2B"), "utf-8");
                if (path.equalsIgnoreCase(parent)) {
                    continue;
                }

                WebDavFile webDavFile = new WebDavFile(path);
                Prop prop = response.getPropstat().getProp();
                webDavFile.setCanon(prop.getDisplayname());
                webDavFile.setCreateTime(0);
                webDavFile.setLastModified(0);
                webDavFile.setSize(prop.getGetcontentlength());
                webDavFile.setIsDirectory(prop.getResourcetype().getCollection() != null);
                webDavFile.exists(true);
                webDavFile.setParent(parent);
                list.add(webDavFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list.toArray(new WebDavFile[list.size()]);
    }

    public InputStream getInputStream() {
        Request.Builder request = new Request.Builder()
                .url(getUrl());

        HttpAuth.Auth auth = HttpAuth.getAuth(url.toString());

        if (auth != null) {
            request.header("Authorization", Credentials.basic(auth.getUser(), auth.getPass()));
        }

        try {
            Response response = okHttpClient.newCall(request.build()).execute();
            return response.body().byteStream();
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据自己的URL，在远程处创建对应的文件夹
     *
     * @return 是否创建成功
     */
    public boolean makeAsDir() {
        Request.Builder request = new Request.Builder()
                .url(getUrl())
                .method("MKCOL", null);
        return execRequest(request);
    }

    /**
     * 下载到本地。若本地存在同名文件，则取消下载
     *
     * @param savedPath 本地的完整路径，包括最后的文件名
     * @return 下载是否成功
     */
    public boolean download(String savedPath) {
        return download(savedPath, false);
    }

    public boolean download(String savedPath, boolean replaceExisting) {
        InputStream in = getInputStream();
        try {
            if (replaceExisting) {
                Files.copy(in, Paths.get(savedPath), StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(in, Paths.get(savedPath));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 上传文件
     *
     * @param localPath 本地文件路径
     * @return 是否成功成功
     */
    public boolean upload(String localPath) {
        File file = new File((localPath));
        String fileMime = "";
        try {
            // 获取文件的MIME类型
            fileMime = Files.probeContentType(Paths.get(localPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 务必注意RequestBody不要嵌套，不然上传时内容可能会被追加多余的文件信息
        RequestBody fileBody = RequestBody.create(MediaType.parse(fileMime), file);
        Request.Builder request = new Request.Builder()
                .url(getUrl())
                .put(fileBody);

        return execRequest(request);
    }

    /**
     * 执行请求，获取响应结果
     *
     * @param requestBuilder 因为还需要追加验证信息，所以此处传递Request.Builder的对象，而不是Request的对象
     * @return 请求执行的结果
     */
    private boolean execRequest(Request.Builder requestBuilder) {
        HttpAuth.Auth auth = HttpAuth.getAuth(url.toString());
        if (auth != null) {
            requestBuilder.header("Authorization", Credentials.basic(auth.getUser(), auth.getPass()));
        }

        try {
            Response response = okHttpClient.newCall(requestBuilder.build()).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 打印对象内的所有属性
     */
    public static <T> T printAllAttrs(String className, Object o) {
        try {
            Class<?> c = Class.forName(className);
            Field[] fields = c.getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);
            }
            System.out.println("=============" + className + "===============");
            for (Field f : fields) {
                String field = f.toString().substring(f.toString().lastIndexOf(".") + 1); //取出属性名称
                System.out.println(field + " --> " + f.get(o));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getCanon() {
        return canon;
    }

    public void setCanon(String canon) {
        this.canon = canon;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getName() {
        return getURLName();
    }

    public String getURLName() {
        if (urlName.isEmpty()) {
            urlName = (parent.isEmpty() ? url.getFile() : url.toString().replace(parent, "")).
                    replace("/", "");
        }
        return urlName;
    }

    public String getHost() {
        return url.getHost();
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public boolean exists() {
        return exists;
    }

    public void exists(boolean exists) {
        this.exists = exists;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String path) {
        parent = path;
    }

    public void setIsDirectory(boolean isDirectory) {
        this.isDirectory = isDirectory;
    }
}