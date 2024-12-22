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
import java.util.Objects;

@Slf4j
@Component
@ApplicationScope
@RequiredArgsConstructor
@Transactional
public class FileService {

    private final FileForDeletionRepository fileForDeletionRepository;
    @Value("${files.path}")
    private String filesPath;
    @Value("${thumbnails.path}")
    private String thumbnailsPath;
    private final FileTrackingRepository fileTrackingRepository;
    private final FileThumbnailRepository fileThumbnailRepository;
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

    public FileTracking saveThumbnail(MultipartFile file) {
        try {
            final var fileSearch = fileTrackingRepository.findFirstByFileName(file.getOriginalFilename());
            if(fileSearch.isPresent()) throw new RuntimeException("File already exists!");
            return saveThumbnailFile(file);
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
                deleteThumbnail(fileFromTracking);
            }
            log.info("File deleted: {}", fileFromTracking.getFileName());
            return gotDeleted;
        } catch (IOException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    private void deleteThumbnail(FileTracking fileFromTracking) {
        final var thumbnail = fileThumbnailRepository.findFirstByForFile(fileFromTracking);
        thumbnail.ifPresent(fileThumbnailRepository::delete);
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
        return saveFileAndSaveTracking(file, fileTracking, filesPath);
    }

    private FileTracking saveThumbnailFile(MultipartFile file) throws IOException {
        final var fileTracking = new FileTracking();
        return saveFileAndSaveTracking(file, fileTracking, thumbnailsPath);
    }

    private FileTracking saveFileAndSaveTracking(MultipartFile file, FileTracking fileTracking, String basePath) throws IOException {
        final var filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        fileTracking.setFileName(file.getOriginalFilename());
        fileTracking.setFilePath(Paths.get(basePath, filename).normalize().toString());
        fileTracking.setFileSize(String.valueOf(file.getSize()));
        fileTracking.setFileType(file.getContentType());
        fileTracking.setTimestamp(Instant.now());

        Files.copy(file.getInputStream(), this.files.resolve(
                Objects.requireNonNull(file.getOriginalFilename()))
        );

        return fileTrackingRepository.save(fileTracking);
    }

    public void saveThumbnail(MultipartFile thumbnail, FileTracking forFile) {
        if(!Objects.requireNonNull(thumbnail.getContentType()).startsWith("image")) throw new RuntimeException("Thumbnail must be an image!");
        fileThumbnailRepository.save(FileThumbnail.builder()
                        .thumbnail(saveThumbnail(thumbnail))
                        .forFile(forFile)
                .build()
        );
    }
}
