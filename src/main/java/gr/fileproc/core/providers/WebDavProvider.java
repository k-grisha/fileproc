package gr.fileproc.core.providers;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.github.sardine.util.SardineUtil;
import gr.fileproc.core.ResourceProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.util.Strings;

public class WebDavProvider extends ResourceProvider {

    private final static String MD5_KEY = "MD5";
    private final Sardine sardine;
    private final String host;
    private final String src;

    //http://192.168.1.150/nextcloud/remote.php/webdav/
    public WebDavProvider() {
        sardine = SardineFactory.begin("4kict", "MashGrish");
        host = "http://192.168.31.150";
        src = "/nextcloud/remote.php/webdav/";
    }

    @Override
    public List<ResourceIdentifier> getIdentifiers(String path) throws Exception {
        var resources = sardine.list(host + src + path, 1);
        return resources.stream()
            .map(r -> new ResourceIdentifier(
                r.getPath().replace(src, ""),
                r.getModified(),
                r.isDirectory(),
                r.isDirectory() ? 0 : r.getContentLength()
            ))
            .collect(Collectors.toList());
    }

    @Override
    public boolean upload(byte[] data, String path) {
        try {
            sardine.put(host + src + path, data);
            return true;
        } catch (IOException e) {
            //todo
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public byte[] download(String path) {
        try {
            return IOUtils.toByteArray(sardine.get(host + src.replace(" ", "%20") + path.replace(" ", "%20")));
        } catch (IOException e) {
            //todo
            e.printStackTrace();
        }
        return new byte[0];
    }

    private InputStream download(DavResource resource) throws IOException {
//URLEncoder.encode(resource.getPath(), StandardCharsets.UTF_8)
//        resource.getPath().replace(" ", "%20");
        return sardine.get(host + resource.getPath().replace(" ", "%20"));
    }

    private String getMd5(DavResource resource) {
        if (resource.isDirectory()) {
            return null;
        }
        var md5 = resource.getCustomProps().get(MD5_KEY);
        if (Strings.isNotBlank(md5)) {
            return md5;
        }
        try {
            md5 = DigestUtils.md5Hex(download(resource));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        try {
            var uri = host + resource.getPath().replace(" ", "%20");
            sardine.setCustomProps(uri, Map.of(MD5_KEY, md5), null);
            sardine.patch(uri,
                Map.of(SardineUtil.createQNameWithDefaultNamespace(MD5_KEY), md5), Collections.emptyList());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return md5;
    }
}
