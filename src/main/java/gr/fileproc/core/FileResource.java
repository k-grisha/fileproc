package gr.fileproc.core;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class FileResource {

    private final String path;
    private final String md5;
    private final boolean directory;
}
