package com.bybrauns.file.filetracking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.annotation.ApplicationScope;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;

@Component
@ApplicationScope
@RequiredArgsConstructor
public class FileTrackingService {
    private final FileTrackingRepository fileTrackingRepository;

    public FileTracking saveFileAndSaveTracking(MultipartFile file, FileTracking fileTracking, String basePath, Path files) throws IOException {
        final var filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        fileTracking.setFileName(file.getOriginalFilename());
        fileTracking.setFilePath(Paths.get(basePath, filename).normalize().toString());
        fileTracking.setFileSize(String.valueOf(file.getSize()));
        fileTracking.setFileType(file.getContentType());
        fileTracking.setTimestamp(Instant.now());

        Files.copy(file.getInputStream(), files.resolve(
                Objects.requireNonNull(filename))
        );

        return fileTrackingRepository.save(fileTracking);
    }
}
