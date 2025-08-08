package gg.nya.imagehosting.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "users")
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores and hyphens")
    private String username;

    @NotNull
    @Size(min = 2, max = 50)
    private String displayName;

    @NotNull
    @Size(min = 8, max = 100)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private List<Role> roles;

    @Column
    private LocalDateTime lastLoginAttempt;

    @NotNull
    private int failedLoginAttempts;

    protected User() {

    }

    public User(String username, String password) {
        this.username = username.toLowerCase();
        this.displayName = username;
        this.password = password;
        this.failedLoginAttempts = 0;
        this.roles = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public LocalDateTime getLastLoginAttempt() {
        return lastLoginAttempt;
    }

    public void setLastLoginAttempt(LocalDateTime lastLoginAttempt) {
        this.lastLoginAttempt = lastLoginAttempt;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }
}
