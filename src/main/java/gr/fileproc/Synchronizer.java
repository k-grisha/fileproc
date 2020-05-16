package gr.fileproc;

import gr.fileproc.core.FileResource;
import gr.fileproc.core.ResourceProvider;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Synchronizer {

    public void sync(ResourceProvider src, ResourceProvider dest, String path) {
        List<FileResource> srcResources = src.getResources(path);
        Map<String, String> md5Map = dest.getResources(path).stream()
            .filter(r -> !r.isDirectory())
            .collect(Collectors.toMap(FileResource::getPath, FileResource::getMd5));

        srcResources.forEach(r -> {
            if (r.isDirectory()) {
                dest.mkdir(r.getPath());
                sync(src, dest, r.getPath());
                return;
            }
            if (md5Map.containsKey(r.getPath()) && md5Map.get(r.getPath()).equals(r.getMd5())) {
                return;
            }
            try {
                var data = src.download(r.getPath());
                dest.uploadFileAndPersistMd5(data, r.getPath());
            } catch (Exception e) {
                log.warn("Fail to sync " + r.getPath() + " from " + src.providerName() + " to " + dest.providerName(), e);
            }

        });
    }

    private Map<String, String> get(List<FileResource> fileResources) {
        return fileResources.stream()
            .collect(Collectors.toMap(FileResource::getPath, FileResource::getMd5));
    }


}
