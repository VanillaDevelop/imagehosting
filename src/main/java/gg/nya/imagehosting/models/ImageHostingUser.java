package gg.nya.imagehosting.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;

@Entity(name = "image_hosting_users")
public class ImageHostingUser implements Serializable {
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
    private HostingMode imageHostingMode;

    @NotNull
    @Column(name = "api_key")
    private String apiKey;

    /**
     * Creates a new ImageHostingUser with the given user and default settings.
     *
     * @param user The user to create the ImageHostingUser for.
     */
    public ImageHostingUser(User user) {
        this.user = user;
        this.imageHostingMode = HostingMode.ALPHANUMERIC;
        this.apiKey = java.util.UUID.randomUUID().toString();
    }

    /**
     * Default constructor for JPA (no properties are set)
     */
    public ImageHostingUser() {

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

    public HostingMode getImageHostingMode() {
        return imageHostingMode;
    }

    public void setImageHostingMode(HostingMode imageHostingMode) {
        this.imageHostingMode = imageHostingMode;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
