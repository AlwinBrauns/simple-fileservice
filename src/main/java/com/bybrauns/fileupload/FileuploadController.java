package com.bybrauns.fileupload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileuploadController {

    private final FileuploadService fileuploadService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileuploadController.class);

    @Autowired
    FileuploadController(FileuploadService fileuploadService) {
        this.fileuploadService = fileuploadService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("fileupload/movie")
    @PreAuthorize("hasAnyRole('user', 'maintainer')")
    public void fileupload(@RequestParam("file") MultipartFile file) {
        log.info("Upload: {}", file.getOriginalFilename());
        fileuploadService.save(file);
    }
}
