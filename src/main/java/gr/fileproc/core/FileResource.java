package gr.fileproc.core;

import lombok.Value;

@Value
public class FileResource {

    private final String path;
    private final String md5;
    private final boolean directory;
    private final long size;

    public FileResource(String path, String md5, boolean directory, long size) {
        this.path = path.trim().replaceAll("^/|/$", "");
        this.md5 = md5;
        this.directory = directory;
        this.size = size;
    }
}
