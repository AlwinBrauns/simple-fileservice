package com.bybrauns.file.filetracking;

import com.bybrauns.file.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.annotation.ApplicationScope;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@Component
@ApplicationScope
@RequiredArgsConstructor
@Slf4j
public class FileTrackingService {
    private final FileTrackingRepository fileTrackingRepository;
    private final ThumbnailService thumbnailService;

    public FileTracking save(MultipartFile file, String filesPath, Path files, String userName) {
        try {
            final var fileSearch = fileTrackingRepository.findFirstByFileNameAndCreatedBy(file.getOriginalFilename(), userName);
            if(fileSearch.isPresent()) throw new RuntimeException("File already exists!");
            return saveFileAndSaveTracking(file, new FileTracking(), filesPath, files, userName);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public boolean delete(String filename, Path files, String userName) {
        try {
            final var fileFromTracking = getFileTracking(filename, userName);
            Path file = files.resolve(fileFromTracking.getFilePath()).normalize();
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

    public FileTracking saveFileAndSaveTracking(MultipartFile file, FileTracking fileTracking, String basePath, Path files, String userName) throws IOException {
        Path userDirectory = Paths.get(basePath, userName);

        if (Files.exists(userDirectory)) {
            if (!Files.isDirectory(userDirectory)) {
                throw new IOException("Der Pfad " + userDirectory + " existiert, ist aber kein Verzeichnis.");
            }
        } else {
            try {
                Files.createDirectories(userDirectory);
            } catch (IOException e) {
                throw new IOException("Konnte das Benutzerverzeichnis nicht erstellen: " + userDirectory, e);
            }
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IOException("Der Original-Dateiname ist null.");
        }
        String filename = Paths.get(originalFilename).getFileName().toString();
        filename = StringUtils.cleanPath(filename);

        if (filename.isEmpty() || filename.contains("..")) {
            throw new IOException("Ungültiger Dateiname: " + filename);
        }

        Path targetPath = userDirectory.resolve(filename).normalize();

        if (Files.exists(targetPath)) {
            throw new IOException("Datei existiert bereits: " + targetPath);
        }

        fileTracking.setFileName(filename);
        fileTracking.setFilePath(targetPath.toString());
        fileTracking.setFileSize(String.valueOf(file.getSize()));
        fileTracking.setFileType(file.getContentType());
        fileTracking.setCreatedBy(userName);
        fileTracking.setTimestamp(Instant.now());

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath);
        } catch (IOException e) {
            throw new IOException("Konnte die Datei nicht nach " + targetPath + " kopieren - " + e.getMessage(), e);
        }

        return fileTrackingRepository.save(fileTracking);
    }

    public FileTracking getFileTracking(String filename, String userName) {
        final var fileSearch = fileTrackingRepository.findFirstByFileNameAndCreatedBy(filename, userName);
        if(fileSearch.isEmpty()) throw new RuntimeException("File not found!");
        return fileSearch.get();
    }
}
