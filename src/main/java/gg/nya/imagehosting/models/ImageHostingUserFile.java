package gg.nya.imagehosting.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity(name = "image_hosting_user_files")
public class ImageHostingUserFile implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    private Long id;

    @NotNull
    @JoinColumn(name = "image_hosting_user_id")
    @OneToOne
    private ImageHostingUser imageHostingUser;

    @NotNull
    @Column(name = "file_name")
    private String fileName;

    @NotNull
    @Column(name = "file_size")
    private Long fileSize;

    @NotNull
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ImageHostingUser getImageHostingUser() {
        return imageHostingUser;
    }

    public void setImageHostingUser(ImageHostingUser imageHostingUser) {
        this.imageHostingUser = imageHostingUser;
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
}
