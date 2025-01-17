package gg.nya.imagehosting.security;

import java.io.Serializable;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserSession implements Serializable {

    private Long userId;
    private String username;
    private boolean authenticated;
    private List<String> roles;

    public void login(Long userId, String username, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
        this.authenticated = true;

        SecurityContextHolder.getContext().setAuthentication(new CustomAuthenticationToken(username, roles));
    }

    public void logout() {
        this.userId = null;
        this.username = null;
        this.authenticated = false;

        SecurityContextHolder.clearContext();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
