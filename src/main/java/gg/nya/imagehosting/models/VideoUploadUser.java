package gg.nya.imagehosting.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;

@Entity(name = "video_upload_users")
public class VideoUploadUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @JoinColumn(name = "user_id")
    @OneToOne
    private User user;

    @NotNull
    @Column(name = "mode")
    @Enumerated(EnumType.STRING)
    private HostingMode videoUploadMode;

    /**
     * Creates a new VideoUploadUser with the given user and default settings.
     *
     * @param user The user to create the VideoUploadUser for.
     */
    public VideoUploadUser(User user) {
        this.user = user;
        this.videoUploadMode = HostingMode.ALPHANUMERIC;
    }

    /**
     * Default constructor for JPA (no properties are set)
     */
    public VideoUploadUser() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public HostingMode getVideoUploadMode() {
        return videoUploadMode;
    }

    public void setVideoUploadMode(HostingMode videoUploadMode) {
        this.videoUploadMode = videoUploadMode;
    }
}
