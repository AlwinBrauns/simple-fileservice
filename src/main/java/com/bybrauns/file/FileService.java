package com.bybrauns.file;

import com.bybrauns.file.filetracking.FileTracking;
import com.bybrauns.file.filetracking.FileTrackingRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
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
import java.util.stream.Stream;

@Component
@ApplicationScope
public class FileService {

    @Value("${files.path}")
    private String filesPath;
    private final FileTrackingRepository fileTrackingRepository;
    Path files;

    @Autowired
    public FileService(FileTrackingRepository fileTrackingRepository) {
        this.fileTrackingRepository = fileTrackingRepository;
    }

    @PostConstruct
    public void init() {
        files = Paths.get(filesPath);
        try {
            Files.createDirectories(files);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    @Transactional
    public void save(MultipartFile file) {
        try {
            final var fileSearch = fileTrackingRepository.findFirstByFileName(file.getOriginalFilename());
            if(fileSearch.isPresent() && !fileSearch.get().isDeleted()) throw new RuntimeException("File already exists!");
            if(fileSearch.isPresent()) {
                saveOnceDeletedFile(file, fileSearch.get());
            } else {
                saveFile(file);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void saveFile(MultipartFile file) throws IOException {
        final var fileTracking = new FileTracking();
        saveFileAndSaveTracking(file, fileTracking);
    }

    private void saveOnceDeletedFile(MultipartFile file, FileTracking fileSearch) throws IOException {
        saveFileAndSaveTracking(file, fileSearch);
    }

    private void saveFileAndSaveTracking(MultipartFile file, FileTracking fileTracking) throws IOException {
        fileTracking.setFileName(file.getOriginalFilename());
        fileTracking.setFilePath(Paths.get(filesPath, file.getOriginalFilename()).toString());
        fileTracking.setFileSize(String.valueOf(file.getSize()));
        fileTracking.setFileType(file.getContentType());
        fileTracking.setTimestamp(Instant.now());
        fileTracking.setDeleted(false);

        Files.copy(file.getInputStream(), this.files.resolve(
                Objects.requireNonNull(file.getOriginalFilename()))
        );

        fileTrackingRepository.save(fileTracking);
    }


    public Resource load(String filename) {
        try {
            filename = StringUtils.cleanPath(filename);
            final var fileSearch = fileTrackingRepository.findFirstByFileName(filename);
            if(fileSearch.isEmpty()) throw new RuntimeException("File not found!");
            if(fileSearch.get().isDeleted()) throw new RuntimeException("File already deleted!");
            final var fileFromTracking = fileSearch.get();
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

    public boolean delete(String filename) {
        try {
            Path file = files.resolve(filename);
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public void deleteAll() {
        FileSystemUtils.deleteRecursively(files.toFile());
    }

    public Stream<Path> loadAll() {
        try (Stream<Path> stream = Files.walk(files, 1).filter(path -> !path.equals(files))) {
            return stream.map(files::relativize);
        } catch (IOException e) {
            throw new RuntimeException("Could not load the files!");
        }
    }

}
