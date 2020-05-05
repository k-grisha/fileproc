package gr.fileproc.core.providers;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.DownloadListener;
import com.yandex.disk.rest.ResourcesArgs.Builder;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.json.Link;
import gr.fileproc.core.ResourceProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
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
//        RestClientource resources = restClient.getResources(new Builder().setPath(path).build());
        return restClient.getResources(new Builder().setPath(path + "/").build()).getResourceList().getItems().stream()
            .map(r -> new ResourceIdentifier(r.getPath().getPath(), r.getModified(), r.isDir(), r.isDir() ? 0 : r.getSize()))
            .collect(Collectors.toList());
    }

    @Override
    public boolean upload(byte[] data, String path) {
        try {
            Link uploadLink = restClient.getUploadLink(path, true);
            File temp = File.createTempFile("pattern", ".suffix");
            OutputStream os = new FileOutputStream(temp);
            os.write(data);
            os.close();

            restClient.uploadFile(uploadLink, false, temp, null);
            Files.readAllBytes(temp.toPath());
        } catch (IOException | ServerException e) {
            // todo
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public byte[] download(String path) {
        try {
            InMemoryStream inMemoryStream = new InMemoryStream();
            restClient.downloadFile(path, inMemoryStream);
            return inMemoryStream.getByteArray();
        } catch (IOException | ServerException e) {
            e.printStackTrace();
        }
        return new byte[0];
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
