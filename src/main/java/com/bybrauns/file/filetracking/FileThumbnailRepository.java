package com.bybrauns.file.filetracking;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface FileThumbnailRepository extends CrudRepository<FileThumbnail, Long> {
    Optional<FileThumbnail> findFirstByForFile(FileTracking fileFromTracking);
}
