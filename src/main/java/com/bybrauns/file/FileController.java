package com.bybrauns.file;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

@RestController
@Slf4j
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final ThumbnailService thumbnailService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("file")
    @PreAuthorize("hasAnyRole('user', 'maintainer')")
    public void fileupload(@RequestParam("file") MultipartFile file, @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail) {
        log.info("Upload: {}", file.getOriginalFilename());
        final var savedFile = fileService.save(file);
        if(thumbnail != null) {
            thumbnailService.saveThumbnail(thumbnail, savedFile);
        }
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("public/clean")
    public void fileclean() {
        fileService.deleteAllMarkedForDeletion();
    }

    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("public/file")
    //@PreAuthorize("hasAnyRole('user', 'maintainer')")
    public void filedeletion(@RequestParam String filename, @RequestParam(required = false) boolean instant) {
        log.info("Deleting: {}", filename);
        if (instant) {
            fileService.delete(filename);
        } else {
            fileService.markAsReadyForDeletion(filename);
        }
    }

    @GetMapping("public/file")
    //@PreAuthorize("hasAnyRole('user', 'maintainer')")
    public void filedownload(HttpServletResponse response, @RequestParam String filename) {
        log.info("Download: {}", filename);
        final Resource resource = fileService.load(filename);
        var contentType = URLConnection.guessContentTypeFromName(resource.getFilename());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + resource.getFilename() + "\""
        );
        try (ServletOutputStream outputStream = response.getOutputStream(); InputStream inputStream = resource.getInputStream()) {
            response.setContentLengthLong(resource.contentLength());
            inputStream.transferTo(outputStream);
            response.setStatus(HttpServletResponse.SC_OK);
            outputStream.flush();
        } catch (IOException e) {
            log.error("File download error for {}: {}", filename, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
