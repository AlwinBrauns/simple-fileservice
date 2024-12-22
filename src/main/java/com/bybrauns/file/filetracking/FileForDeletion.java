package com.bybrauns.file.filetracking;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileForDeletion {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "file_tracking_id")
    FileTracking fileTracking;
    Instant timestamp;

}
