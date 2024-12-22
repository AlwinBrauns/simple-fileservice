package com.bybrauns.file;

import com.bybrauns.file.filetracking.*;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.annotation.ApplicationScope;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@Slf4j
@Component
@ApplicationScope
@RequiredArgsConstructor
@Transactional
public class FileService {

    @Value("${files.path}")
    private String filesPath;
    private final FileTrackingRepository fileTrackingRepository;
    private final FileForDeletionRepository fileForDeletionRepository;
    private final ThumbnailService thumbnailService;
    private final FileTrackingService fileTrackingService;
    Path files;

    @PostConstruct
    public void init() {
        files = Paths.get(filesPath);
        try {
            Files.createDirectories(files);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    public FileTracking save(MultipartFile file) {
        try {
            final var fileSearch = fileTrackingRepository.findFirstByFileName(file.getOriginalFilename());
            if(fileSearch.isPresent()) throw new RuntimeException("File already exists!");
            return saveFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public Resource load(String filename) {
        try {
            filename = StringUtils.cleanPath(filename);
            final var fileFromTracking = getFileTracking(filename);
            Path file = files.resolve("%s/%s".formatted(filesPath, fileFromTracking.getFileName())).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public void markAsReadyForDeletion(String filename) {
        final var fileFromTracking = getFileTracking(filename);
        if(fileForDeletionRepository.findFirstByFileTracking(fileFromTracking).isPresent())
            throw new RuntimeException("File already marked for deletion!");
        fileForDeletionRepository.save(
                FileForDeletion.builder()
                        .fileTracking(fileFromTracking)
                        .timestamp(Instant.now())
                .build()
        );
    }

    public boolean delete(String filename) {
        try {
            final var fileFromTracking = getFileTracking(filename);
            Path file = files.resolve(fileFromTracking.getFileName()).normalize();
            final var gotDeleted = Files.deleteIfExists(file);
            if(gotDeleted) {
                fileTrackingRepository.delete(fileFromTracking);
                thumbnailService.deleteThumbnail(fileFromTracking);
            }
            log.info("File deleted: {}", fileFromTracking.getFileName());
            return gotDeleted;
        } catch (IOException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public void deleteAllMarkedForDeletion() {
        final var allMarkedForDeletions = fileForDeletionRepository.findAll();
        allMarkedForDeletions.forEach(markAsReadyForDeletion -> {
            if(delete(markAsReadyForDeletion.getFileTracking().getFileName())) {
                fileForDeletionRepository.delete(markAsReadyForDeletion);
            }
        });
    }

    private FileTracking getFileTracking(String filename) {
        final var fileSearch = fileTrackingRepository.findFirstByFileName(filename);
        if(fileSearch.isEmpty()) throw new RuntimeException("File not found!");
        return fileSearch.get();
    }

    private FileTracking saveFile(MultipartFile file) throws IOException {
        final var fileTracking = new FileTracking();
        return fileTrackingService.saveFileAndSaveTracking(file, fileTracking, filesPath, files);
    }

}
