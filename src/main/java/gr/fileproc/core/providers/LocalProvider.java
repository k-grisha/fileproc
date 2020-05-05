package gr.fileproc.core.providers;

import gr.fileproc.core.ResourceProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


public class LocalProvider extends ResourceProvider {

    private final String src;

    public LocalProvider() {
        src = System.getProperty("user.home") + "/fotoSrc/";
    }

    // ходим только в текущей дериктории
    @Override
    public List<ResourceIdentifier> getIdentifiers(String path) throws IOException {
        return Files.walk(Paths.get(src + path), 1)
            .map(p -> new ResourceIdentifier(p.toString().replace(src, ""),
                getLastModifiedDate(p),
                Files.isDirectory(p),
                Files.isDirectory(p) ? 0 : getSize(p)
            ))
            .collect(Collectors.toList());
    }

    @Override
    public boolean upload(byte[] data, String path) {
        try {
            Files.write(Paths.get(src + path), data);
        } catch (IOException e) {
            // todo
            throw new RuntimeException(e);
        }

        return true;
    }

    @Override
    public byte[] download(String path) {
        try {
            return Files.readAllBytes(Paths.get(src + path));
        } catch (IOException e) {
            // todo
            throw new RuntimeException(e);
        }
    }

    private long getSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            //todo
            e.printStackTrace();
            return 0;
        }
    }

    private Date getLastModifiedDate(Path path) {
        try {
            return new Date(Files.getLastModifiedTime(path).toMillis());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
//        Files.readAttributes(path, BasicFileAttributes.class).lastModifiedTime()
    }

}
