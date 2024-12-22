package com.bybrauns.file;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

@Component
@ApplicationScope
@RequiredArgsConstructor
public class FileScheduler {
    private final FileService fileService;
    @Scheduled(cron = "0 0 0 * * 0")
    void deletion() {
        fileService.deleteAllMarkedForDeletion();
    }
}
