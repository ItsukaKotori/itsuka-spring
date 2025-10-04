package org.itsuka.file;

import org.itsuka.file.minio.MinioConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({MinioConfiguration.class})
public class FileAutoConfiguration {
}
