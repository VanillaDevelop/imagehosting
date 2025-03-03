package gg.nya.imagehosting.security;

import java.util.UUID;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class SessionCsrfTokenRepository implements CsrfTokenRepository {
    private static final String CSRF_TOKEN_ATTR_NAME = "_csrf";

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", UUID.randomUUID().toString());
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        if (token == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute(CSRF_TOKEN_ATTR_NAME);
            }
        } else {
            HttpSession session = request.getSession();
            session.setAttribute(CSRF_TOKEN_ATTR_NAME, token.getToken());
        }
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        String token = (String) session.getAttribute(CSRF_TOKEN_ATTR_NAME);
        if (token == null) {
            return null;
        }

        return new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", token);
    }
}