package gr.fileproc.core.providers;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.ResourcesArgs.Builder;
import com.yandex.disk.rest.RestClient;
import gr.fileproc.core.ResourceProvider;
import java.util.List;
import java.util.stream.Collectors;

public class YaProvider extends ResourceProvider {

    private final RestClient restClient;

    public YaProvider() {
        var credentials = new Credentials("ya4kict", "OAuth AgAAAAADFpZzAAZJoIptLnuoKUY1g-qz0NCA1W8");
        restClient = new RestClient(credentials);
    }

    @Override
    public List<ResourceIdentifier> getIdentifiers(String path) throws Exception {
//        Resource resources = restClient.getResources(new Builder().setPath(path).build());
        return restClient.getResources(new Builder().setPath("/" + path).build()).getResourceList().getItems().stream()
            .map(r -> new ResourceIdentifier(r.getPath().getPath(), r.getModified(), r.isDir()))
            .collect(Collectors.toList());
    }

    @Override
    public boolean upload(byte[] data, String path) {
        return false;
    }

    @Override
    public byte[] download(String path) {
        return new byte[0];
    }
}
