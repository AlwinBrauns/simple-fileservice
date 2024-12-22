package com.bybrauns.file.filetracking;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface FileForDeletionRepository extends CrudRepository<FileForDeletion, Long> {
    Optional<FileForDeletion> findFirstByFileTracking(FileTracking fileFromTracking);
}
