package gr.fileproc.core.providers;

import gr.fileproc.core.ResourceProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalProvider extends ResourceProvider {

    private final String src;

    public LocalProvider(String dir) {
        src = System.getProperty("user.home") + dir;
    }

    // ходим только в текущей дериктории
    @Override
    public List<ResourceIdentifier> getIdentifiers(String path) throws Exception {
        try {
            return Files.walk(Paths.get(src + path), 1)
                .map(this::getResourceIdentifierFrom)
                .collect(Collectors.toList());
        } catch (NoSuchFileException e) {
            log.warn("No such file " + path);
            return Collections.emptyList();
        }
    }

    @Override
    public ResourceIdentifier upload(byte[] data, String path) throws Exception {
        var savedPath = Files.write(Paths.get(src + path), data);
        return getResourceIdentifierFrom(savedPath);
    }

    @Override
    public byte[] download(String path) throws Exception {
        return Files.readAllBytes(Paths.get(src + path));
    }

    @Override
    public String getRootPath() {
        return src;
    }

    @Override
    public String providerName() {
        return "LocalDisk";
    }

    @Override
    public void mkdir(String path) {
        new File(src + path).mkdirs();
    }

    private ResourceIdentifier getResourceIdentifierFrom(Path p) {
        return new ResourceIdentifier(p.toString().replace(src, ""),
            getLastModifiedDate(p),
            Files.isDirectory(p),
            Files.isDirectory(p) ? 0 : getSize(p)
        );
    }

    private long getSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            log.error("Can't get File size " + path, e);
            return 0;
        }
    }

    private Date getLastModifiedDate(Path path) {
        try {
            return new Date(Files.getLastModifiedTime(path).toMillis());
        } catch (IOException e) {
            log.error("Cant get last modified date " + path, e);
        }
        return null;
    }

}
