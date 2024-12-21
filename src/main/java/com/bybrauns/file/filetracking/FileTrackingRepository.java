package com.bybrauns.file.filetracking;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface FileTrackingRepository extends CrudRepository<FileTracking, Long> {
    Optional<FileTracking> findFirstByFileName(String fileName);
}
