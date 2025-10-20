package gg.nya.imagehosting.config;

import gg.nya.imagehosting.security.CustomAuthenticationToken;
import gg.nya.imagehosting.security.SessionCsrfTokenRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.util.function.Supplier;

/**
 * Security configuration for the application.
 */
@Configuration
public class SecurityConfig {

    private final SessionCsrfTokenRepository csrfTokenRepository;

    public SecurityConfig(SessionCsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Public resources
                        .requestMatchers(
                                "/javax.faces.resource/**",
                                "/jakarta.faces.resource/**",
                                "/assets/**",
                                "/index.xhtml",
                                "/",
                                "/i/**",
                                "/v/**",
                                "/thumbnails/**",
                                "/videodisplay.xhtml",
                                "/error.xhtml",
                                "/error",
                                "/favicon.ico"
                        )
                        .permitAll()
                        //Admin role protected
                        .requestMatchers("/admin.xhtml")
                        .access((authentication, context) -> new AuthorizationDecision(hasRole(authentication, "ADMIN")))
                        // Image hosting role protected
                        .requestMatchers("/imagehosting.xhtml")
                        .access((authentication, context) -> new AuthorizationDecision(hasRole(authentication, "IMAGE_HOSTING")))
                        // Video upload role protected
                        .requestMatchers("/videoupload.xhtml")
                        .access((authentication, context) -> new AuthorizationDecision(hasRole(authentication, "VIDEO_UPLOAD")))
                        .anyRequest()
                        .authenticated())
                // Set security context to update without explicit save
                .securityContext(securityContext -> securityContext.requireExplicitSave(false))
                // CSRF configuration
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // Ignoring the REST endpoint that is called from outside the application
                        .ignoringRequestMatchers("/i/**"));

        return http.build();
    }

    /**
     * Check if the authenticated user has the provided role.
     * @param authentication the authentication supplier
     * @param role the role to check, as a String
     * @return true if the user has the role, false otherwise
     */
    private boolean hasRole(Supplier<Authentication> authentication, String role) {
        if (!(authentication.get() instanceof CustomAuthenticationToken customAuthenticationToken))
            return false;
        return customAuthenticationToken.getRoles().contains(role);
    }
}
