package gg.nya.imagehosting.security;

import java.io.Serializable;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserSession implements Serializable {

    private String userId;
    private String username;
    private boolean authenticated;

    public void login(String userId, String username) {
        this.userId = userId;
        this.username = username;
        this.authenticated = true;

        SecurityContextHolder.getContext().setAuthentication(new CustomAuthenticationToken(username));
    }

    public void logout() {
        this.userId = null;
        this.username = null;
        this.authenticated = false;

        SecurityContextHolder.clearContext();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
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
}
