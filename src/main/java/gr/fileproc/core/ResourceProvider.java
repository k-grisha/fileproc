package gr.fileproc.core;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class ResourceProvider {

    private final static String MD5_MAP_FILE = ".mdproc.ser";

    public List<FileResource> getResources(String path) throws Exception {
        List<ResourceIdentifier> identifiers;
        try {
            identifiers = getIdentifiers(trimSlashes(path));
        } catch (Exception e) {
            log.warn("Cant read list of resources from " + providerName(), e);
            return Collections.emptyList();
        }

        var resources = identifiers.stream()
            .filter(identifier -> isValid(identifier, path))
            .collect(Collectors.toList());

        var mdMap = getMd5Map(path, resources);
        return resources.stream()
            .filter(r -> !r.getFilename().equals(MD5_MAP_FILE))
            .map(r -> new FileResource(trimSlashes(r.getPath()),
                r.isFile() ? mdMap.get(r.path).get(1) : null,
                r.directory,
                r.size))
            .sorted(Comparator.comparing(FileResource::getPath))
            .collect(Collectors.toList());
    }

    private boolean isValid(ResourceIdentifier identifier, String path) {
        return identifier != null
            && identifier.getFilename() != null
            && identifier.getPath() != null
            && !trimSlashes(identifier.getFilename()).isBlank()
            && !trimSlashes(identifier.getPath()).isBlank()
            && (!identifier.getFilename().startsWith(".") || identifier.getFilename().equals(MD5_MAP_FILE))
            && !trimSlashes(identifier.getPath()).equals(trimSlashes(getRootPath()))
            && !trimSlashes(identifier.getPath()).equals(trimSlashes(path));
    }

    private String trimSlashes(String str) {
        return str.trim().replaceAll("^/|/$", "");
    }

    private Map<String, List<String>> getMd5Map(String path, List<ResourceIdentifier> resources) {
        if (resources.stream().allMatch(ResourceIdentifier::isDirectory)) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> persistedMd5Map = readPersistMd5Map(resources);
        HashMap<String, List<String>> md5Map = new HashMap<>();
        resources.stream()
            .filter(ResourceIdentifier::isFile)
            .filter(r -> !r.getFilename().equals(MD5_MAP_FILE))
            .forEach(identRes -> {
                var pair = persistedMd5Map.get(identRes.getPath());
                if (isPersistedHashValid(identRes, pair)) {
                    md5Map.put(identRes.getPath(), pair);
                    return;
                }
                try {
                    var md5 = DigestUtils.md5Hex(download(identRes.getPath()));
                    md5Map.put(identRes.getPath(), List.of(identRes.getModifiedAsString(), md5));
                } catch (Exception e) {
                    log.warn("Can't download file " + identRes.getPath() + " by " + providerName() + " to calculate MD5");
                }

            });

        if (!Objects.deepEquals(md5Map, persistedMd5Map)) {
            try {
                upload(SerializationUtils.serialize(md5Map), path + MD5_MAP_FILE);
            } catch (Exception e) {
                log.error("Unable to update " + MD5_MAP_FILE + " file by " + providerName(), e);
            }
        }
        return md5Map;
    }


    private boolean isPersistedHashValid(ResourceIdentifier resource, List<String> dateMd5) {
        if (dateMd5 == null || dateMd5.size() != 2 || StringUtils.isBlank(dateMd5.get(0))) {
            return false;
        }
        return resource.getModifiedAsString().equals(dateMd5.get(0));
    }

    private Map<String, List<String>> readPersistMd5Map(List<ResourceIdentifier> resources) {
        Optional<ResourceIdentifier> mdMapFile = resources.stream()
            .filter(r -> r.path.endsWith(MD5_MAP_FILE))
            .findFirst();
        if (mdMapFile.isEmpty()) {
            return Collections.emptyMap();
        }
        byte[] data;
        try {
            data = download(mdMapFile.get().path);
        } catch (Exception e) {
            log.warn("Cant download file " + mdMapFile.get().path + " by " + providerName(), e);
            return Collections.emptyMap();
        }
        try {
            return SerializationUtils.deserialize(data);
        } catch (RuntimeException e) {
            log.warn("Cant deserialize file " + mdMapFile.get().path + " from " + providerName(), e);
            return Collections.emptyMap();
        }
    }

    abstract public List<ResourceIdentifier> getIdentifiers(String path) throws Exception;

    abstract public void upload(byte[] data, String path) throws Exception;

    abstract public byte[] download(String path) throws Exception;

    abstract public String getRootPath();

    abstract public String providerName();


    @Getter
    public static class ResourceIdentifier {

        private final String path;
        private final Date modified;
        private final boolean directory;
        private final LocalDateTime modifiedLocalDateTime;
        private final String filename;
        private final long size;

        public ResourceIdentifier(String path, Date modified, boolean directory, Long size) {
            this.path = path;
            this.modified = modified;
            this.directory = directory;
            this.modifiedLocalDateTime = modified == null ? null : new java.sql.Timestamp(modified.getTime()).toLocalDateTime();
            this.filename = Paths.get(path).getFileName().toString();
            this.size = size == null ? 0 : size;
        }

        public boolean isFile() {
            return !directory;
        }

        public String getModifiedAsString() {
            return getModifiedLocalDateTime().format(DateTimeFormatter.ISO_DATE_TIME);
        }


    }
}
