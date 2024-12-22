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
import java.util.Objects;

@Slf4j
@Component
@ApplicationScope
@RequiredArgsConstructor
@Transactional
public class ThumbnailService {

    @Value("${thumbnails.path}")
    private String thumbnailsPath;
    private final FileTrackingRepository fileTrackingRepository;
    private final FileThumbnailRepository fileThumbnailRepository;
    private final FileTrackingService fileTrackingService;
    Path files;

    @PostConstruct
    public void init() {
        files = Paths.get(thumbnailsPath);
        try {
            Files.createDirectories(files);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    public Resource load(String filename) {
        try {
            filename = StringUtils.cleanPath(filename);
            final var fileFromTracking = getFileTracking(filename);
            final var thumbnail = fileThumbnailRepository.findFirstByForFile(fileFromTracking).orElseThrow();
            Path file = files.resolve("%s/%s".formatted(thumbnailsPath, thumbnail.getThumbnail().getFileName())).normalize();
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

    private FileTracking getFileTracking(String filename) {
        final var fileSearch = fileTrackingRepository.findFirstByFileName(filename);
        if(fileSearch.isEmpty()) throw new RuntimeException("File not found!");
        return fileSearch.get();
    }

    public void saveThumbnail(MultipartFile thumbnail, FileTracking forFile) {
        if(!Objects.requireNonNull(thumbnail.getContentType()).startsWith("image")) throw new RuntimeException("Thumbnail must be an image!");
        fileThumbnailRepository.save(FileThumbnail.builder()
                .thumbnail(save(thumbnail))
                .forFile(forFile)
                .build()
        );
    }

    public FileTracking save(MultipartFile file) {
        try {
            final var fileSearch = fileTrackingRepository.findFirstByFileName(file.getOriginalFilename());
            if(fileSearch.isPresent()) throw new RuntimeException("File already exists!");
            return saveThumbnailFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void deleteThumbnail(FileTracking fileFromTracking) {
        final var thumbnail = fileThumbnailRepository.findFirstByForFile(fileFromTracking);
        thumbnail.ifPresent(fileThumbnailRepository::delete);
    }

    private FileTracking saveThumbnailFile(MultipartFile file) throws IOException {
        final var fileTracking = new FileTracking();
        return fileTrackingService.saveFileAndSaveTracking(file, fileTracking, thumbnailsPath, files);
    }

}
