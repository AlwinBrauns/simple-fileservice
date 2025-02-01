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
    private final FileForDeletionRepository fileForDeletionRepository;
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

    public FileTracking save(MultipartFile file, String userName) {
        return fileTrackingService.save(file, filesPath, files, userName);
    }

    public Resource load(String filename, String userName) {
        try {
            filename = StringUtils.cleanPath(filename);
            final var fileFromTracking = fileTrackingService.getFileTracking(filename, userName);
            Path file = files.resolve("%s/%s/%s".formatted(filesPath, userName, fileFromTracking.getFileName())).normalize();
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

    public void markAsReadyForDeletion(String filename, String userName) {
        final var fileFromTracking = fileTrackingService.getFileTracking(filename, userName);
        if(fileForDeletionRepository.findFirstByFileTracking(fileFromTracking).isPresent())
            throw new RuntimeException("File already marked for deletion!");
        fileForDeletionRepository.save(
                FileForDeletion.builder()
                        .fileTracking(fileFromTracking)
                        .timestamp(Instant.now())
                .build()
        );
    }

    public void deleteAllMarkedForDeletion() {
        final var allMarkedForDeletions = fileForDeletionRepository.findAll();
        allMarkedForDeletions.forEach(markAsReadyForDeletion -> {
            if(fileTrackingService.delete(markAsReadyForDeletion.getFileTracking().getFileName(), files, markAsReadyForDeletion.getFileTracking().getCreatedBy())) {
                fileForDeletionRepository.delete(markAsReadyForDeletion);
            }
        });
    }

    public boolean delete(String filename, String userName) {
        return fileTrackingService.delete(filename, files, userName);
    }
}
