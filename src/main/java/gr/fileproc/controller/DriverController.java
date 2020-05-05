package gr.fileproc.controller;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.DownloadListener;
import com.yandex.disk.rest.ResourcesArgs.Builder;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.json.Link;
import com.yandex.disk.rest.json.Resource;
import gr.fileproc.core.FileResource;
import gr.fileproc.core.ResourceProvider;
import gr.fileproc.core.providers.LocalProvider;
import gr.fileproc.core.providers.WebDavProvider;
import gr.fileproc.core.providers.YaProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DriverController {

    String home = System.getProperty("user.home");

    ResourceProvider local = new LocalProvider();
    ResourceProvider webDav = new WebDavProvider();
    ResourceProvider yandex = new YaProvider();

    @GetMapping("get-local")
    public List<FileResource> getLocal() throws Exception {
        return local.getResources("");
    }

    @GetMapping("get-dav")
    public List<FileResource> getWebDav() throws Exception {
        return webDav.getResources("");
    }

    @GetMapping("get-ya")
    public List<FileResource> getYa() throws Exception {
        return yandex.getResources("");
    }

    @GetMapping({"/file/{path}", "/file"})
    public List<String> getFiles(@PathVariable(required = false) String path) throws IOException {
        path = path == null ? "" : path;

        var src = home + "/fotoSrc";
        var distDir = home + "/fotoDest";

        var result = new ArrayList<String>();
        Files.walkFileTree((Paths.get(src)), new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                var dist = Paths.get(dir.toString().replace(src, distDir));
                if (dir.toString().equals(src) ||
                    Files.exists(dist)) {
                    return FileVisitResult.CONTINUE;
                }

                Files.createDirectory(dist);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                var dist = Paths.get(path.toString().replace(src, distDir));

                if (Files.exists(dist) && !destinationNeedUpdate(attrs, dist)) {
                    return FileVisitResult.CONTINUE;
                }

                Files.copy(path, dist, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                result.add(path.toString());
                return FileVisitResult.CONTINUE;
            }

            private boolean destinationNeedUpdate(BasicFileAttributes srcAttrs, Path dist) throws IOException {
                var distAttr = Files.readAttributes(dist, BasicFileAttributes.class);
                return !distAttr.lastModifiedTime().equals(srcAttrs.lastModifiedTime()) ||
                    distAttr.size() != srcAttrs.size();
            }

        });

        return result;

    }

    @GetMapping("webdav")
    public List<String> getDav() throws IOException {
        var result = new ArrayList<String>();
        Sardine sardine = SardineFactory.begin("admin", "MashGrish");
        List<DavResource> resources = sardine.list("http://192.168.1.150/nextcloud/remote.php/webdav/", 1);
        for (DavResource res : resources) {
            result.add(res.getPath());
        }
        return result;
    }

    @GetMapping("webdavsave")
    public List<String> davSave() throws IOException {
        var result = new ArrayList<String>();
        Sardine sardine = SardineFactory.begin("admin", "MashGrish");
//        List<DavResource> resources = sardine.list("http://192.168.1.150/nextcloud/remote.php/webdav/",-1);
//        byte[] data = FileUtils.readFileToByteArray(new File(home + "/pgadmin.dmg"));
        byte[] data = FileUtils.readFileToByteArray(new File(home + "/posobia.tiff"));
        sardine.put("http://192.168.1.150/nextcloud/remote.php/webdav/posobia.tiff", data);
        return result;
    }

    @GetMapping("yadav")
    public List<String> getYaDav() throws IOException {
        var result = new ArrayList<String>();
        Sardine sardine = SardineFactory.begin("ya4kict", "yanMashGrishdex");
        List<DavResource> resources = sardine.list("https://webdav.yandex.ru", 1);
        for (DavResource res : resources) {
            result.add(res.getPath());
        }
        return result;
    }

    @GetMapping("yadavsave")
    public List<String> yadavSave() throws IOException, NoSuchAlgorithmException {
        var result = new ArrayList<String>();
        Sardine sardine = SardineFactory.begin("ya4kict", "yanMashGrishdex");
//        List<DavResource> resources = sardine.list("http://192.168.1.150/nextcloud/remote.php/webdav/",-1);
//        byte[] data = FileUtils.readFileToByteArray(new File(home + "/pgadmin.dmg"));

        byte[] data = FileUtils.readFileToByteArray(new File(home + "/posobia.tiff"));

        MessageDigest md = MessageDigest.getInstance("MD5");
        DigestInputStream dis = new DigestInputStream(new ByteArrayInputStream(data), md);
        byte[] digest = md.digest();
        sardine.put("https://webdav.yandex.ru/posobia.tiff", data);
        return result;
    }


    // https://oauth.yandex.ru/authorize?response_type=token&client_id=d2dda70c1d0e4a119e4c0c13bd48ae74
    // http://localhost:8080/ya#access_token=AgAAAAADFpZzAAZJoIptLnuoKUY1g-qz0NCA1W8&token_type=bearer&expires_in=31535999
    // http://localhost:8080/ya#access_token=AgAAAAADFpZzAAZJoIptLnuoKUY1g-qz0NCA1W8&token_type=bearer&expires_in=31533862
    @GetMapping("ya")
    public List<String> getYandex() throws IOException, ServerIOException {
        Credentials credentials = new Credentials("ya4kict", "OAuth AgAAAAADFpZzAAZJoIptLnuoKUY1g-qz0NCA1W8");
        RestClient restClient = new RestClient(credentials);
//        var  diskInfo= restClient.getDiskInfo();
        Resource resources = restClient.getResources(new Builder().setPath("/").build());
        try {
            InMemoryStream inMemoryStream = new InMemoryStream();
            restClient.downloadFile("posobia.tiff", inMemoryStream);
//            OutputStream outputStream = inMemoryStream.getOutputStream(true);
            byte[] byteArray = inMemoryStream.getByteArray();
            System.out.println(byteArray.length);
            return null;
        } catch (ServerException e) {
            e.printStackTrace();
        }
        System.out.println(resources);
        var result = new ArrayList<String>();

        return result;
    }

    @GetMapping("yasave")
    public List<String> yasave() throws IOException, ServerException {
        Credentials credentials = new Credentials("ya4kict", "OAuth AgAAAAADFpZzAAZJoIptLnuoKUY1g-qz0NCA1W8");
        RestClient restClient = new RestClient(credentials);
//        var  diskInfo= restClient.getDiskInfo();

        Link uploadLink = restClient.getUploadLink("/pgadmin.dmg", false);
        restClient.uploadFile(uploadLink, false, new File(home + "/pgadmin.dmg"), null);

        var result = new ArrayList<String>();

        return result;
    }

    public static class InMemoryStream extends DownloadListener {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        @Override
        public OutputStream getOutputStream(boolean append) throws IOException {
            return byteArrayOutputStream;
        }

        byte[] getByteArray() {
            return byteArrayOutputStream.toByteArray();
        }

    }


}
