package gr.fileproc.core.providers;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.github.sardine.impl.SardineException;
import com.github.sardine.util.SardineUtil;
import gr.fileproc.core.ResourceProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.logging.log4j.util.Strings;

@Slf4j
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
        List<DavResource> resources = null;
        try {
            resources = sardine.list(host + src + path, 1);
        } catch (HttpResponseException e) {
            log.warn("Unable to read path " + path + ". HTTPCode:" + e.getStatusCode() + ". Message:" + e.getReasonPhrase());
            return Collections.emptyList();
        }
        return resources.stream()
            .map(r -> new ResourceIdentifier(
                r.getPath().replace(src, ""),
                r.getModified(),
                r.isDirectory(),
                r.isDirectory() ? 0 : r.getContentLength()))
            .collect(Collectors.toList());
    }

    @Override
    public ResourceIdentifier upload(byte[] data, String path) throws Exception {
        sardine.put(host + src + path, data);
        return null;
    }

    @Override
    public byte[] download(String path) throws Exception {
        return IOUtils.toByteArray(sardine.get(host + src.replace(" ", "%20") + path.replace(" ", "%20")));
    }

    @Override
    public String getRootPath() {
        return src;
    }

    @Override
    public String providerName() {
        return "WebDav";
    }

    private InputStream download(DavResource resource) throws IOException {
//URLEncoder.encode(resource.getPath(), StandardCharsets.UTF_8)
//        resource.getPath().replace(" ", "%20");
        return sardine.get(host + resource.getPath().replace(" ", "%20"));
    }

    @Override
    public void mkdir(String path) {

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
