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
import java.security.Principal;

@RestController
@Slf4j
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final ThumbnailService thumbnailService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("file")
    @PreAuthorize("hasAnyRole('maintainer')")
    public void fileupload(Principal principal, @RequestParam("file") MultipartFile file, @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail) {
        final var savedFile = fileService.save(file, principal.getName());
        if(thumbnail != null) {
            thumbnailService.save(thumbnail, savedFile, principal.getName());
        }
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("clean")
    @PreAuthorize("hasAnyRole('maintainer')")
    public void fileclean() {
        fileService.deleteAllMarkedForDeletion();
    }

    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("file")
    @PreAuthorize("hasAnyRole('maintainer')")
    public void filedeletion(Principal principal, @RequestParam String filename, @RequestParam(required = false) boolean instant) {
        log.info("Deleting: {}", filename);
        if (instant) {
            if(!fileService.delete(filename, principal.getName())) {
                throw new RuntimeException("Could not delete file: " + filename);
            }
        } else {
            fileService.markAsReadyForDeletion(filename, principal.getName());
        }
    }

    @GetMapping("file")
    @PreAuthorize("hasAnyRole('maintainer')")
    public void filedownload(Principal principal, HttpServletResponse response, @RequestParam String filename) {
        loadFile(response, filename, fileService.load(filename, principal.getName()));
    }

    @GetMapping("thumbnail")
    @PreAuthorize("hasAnyRole('maintainer')")
    public void thumbnaildownload(Principal principal, HttpServletResponse response, @RequestParam String filename) {
        loadFile(response, filename, thumbnailService.load(filename, principal.getName()));
    }

    private void loadFile(HttpServletResponse response, String filename, Resource resource) {
        log.info("Download: {}", filename);
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
