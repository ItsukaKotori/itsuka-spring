package org.itsuka.file;

import org.itsuka.file.minio.MinioConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({MinioConfiguration.class})
public class FileAutoConfiguration {
}
