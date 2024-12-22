package com.bybrauns.file.filetracking;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileThumbnail {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "thumbnail_id")
    FileTracking thumbnail;
    @ManyToOne
    @JoinColumn(name = "forfile_id")
    FileTracking forFile;
}
