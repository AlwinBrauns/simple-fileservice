package com.bybrauns.file;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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
public class FileController {

    private final FileService fileService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileController.class);

    @Autowired
    FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("file")
    @PreAuthorize("hasAnyRole('user', 'maintainer')")
    public void fileupload(@RequestParam("file") MultipartFile file) {
        log.info("Upload: {}", file.getOriginalFilename());
        fileService.save(file);
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
