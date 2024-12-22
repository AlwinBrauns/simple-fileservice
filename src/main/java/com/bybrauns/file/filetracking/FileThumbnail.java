package com.bybrauns.file.filetracking;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

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
    @Cascade(CascadeType.DELETE_ORPHAN)
    FileTracking thumbnail;
    @ManyToOne
    @JoinColumn(name = "forfile_id")
    FileTracking forFile;
}
