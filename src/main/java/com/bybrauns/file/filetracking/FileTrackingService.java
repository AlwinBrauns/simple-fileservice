package com.bybrauns.file.filetracking;

import com.bybrauns.file.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class FileTrackingService {
    private final FileTrackingRepository fileTrackingRepository;
    private final ThumbnailService thumbnailService;

    public FileTracking save(MultipartFile file, String filesPath, Path files) {
        try {
            final var fileSearch = fileTrackingRepository.findFirstByFileName(file.getOriginalFilename());
            if(fileSearch.isPresent()) throw new RuntimeException("File already exists!");
            return saveFileAndSaveTracking(file, new FileTracking(), filesPath, files);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public boolean delete(String filename, Path files) {
        try {
            final var fileFromTracking = getFileTracking(filename);
            Path file = files.resolve(fileFromTracking.getFileName()).normalize();
            final var gotDeleted = Files.deleteIfExists(file);
            if(gotDeleted) {
                thumbnailService.deleteThumbnail(fileFromTracking);
                fileTrackingRepository.delete(fileFromTracking);
            }
            log.info("File deleted: {}", fileFromTracking.getFileName());
            return gotDeleted;
        } catch (IOException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

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

    public FileTracking getFileTracking(String filename) {
        final var fileSearch = fileTrackingRepository.findFirstByFileName(filename);
        if(fileSearch.isEmpty()) throw new RuntimeException("File not found!");
        return fileSearch.get();
    }
}
