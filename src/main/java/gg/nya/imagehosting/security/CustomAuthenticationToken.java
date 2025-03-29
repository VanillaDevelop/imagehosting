package gg.nya.imagehosting.security;

import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class CustomAuthenticationToken extends AbstractAuthenticationToken {

    private final Long userId;
    private final String username;
    private final List<String> roles;

    public CustomAuthenticationToken(Long userId, String username, List<String> roles) {
        super(null);
        this.userId = userId;
        this.username = username;
        this.roles = roles;
        setAuthenticated(true);
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    public List<String> getRoles() {
        return roles;
    }

    public Long getUserId() {
        return userId;
    }
}
