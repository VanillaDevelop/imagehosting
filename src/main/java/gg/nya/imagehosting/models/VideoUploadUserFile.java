package gg.nya.imagehosting.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity(name = "video_upload_user_files")
public class VideoUploadUserFile implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @JoinColumn(name = "video_upload_user_id")
    @ManyToOne
    private VideoUploadUser videoUploadUser;

    @NotNull
    @Column(name = "file_name")
    private String fileName;

    @NotNull
    @Column(name = "file_size")
    private Long fileSize;

    @NotNull
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "upload_status")
    @Enumerated(EnumType.STRING)
    private VideoUploadStatus uploadStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VideoUploadUser getVideoUploadUser() {
        return videoUploadUser;
    }

    public void setVideoUploadUser(VideoUploadUser videoUploadUser) {
        this.videoUploadUser = videoUploadUser;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public VideoUploadStatus getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(VideoUploadStatus uploadStatus) {
        this.uploadStatus = uploadStatus;
    }
}
