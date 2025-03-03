package gg.nya.imagehosting.config;

import gg.nya.imagehosting.security.CustomAuthenticationToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.util.function.Supplier;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/javax.faces.resource/**", "/index.xhtml", "/", "/error")
                        .permitAll()
                        .requestMatchers("/admin.xhtml")
                        .access((authentication, context) -> new AuthorizationDecision(hasAdminRole(authentication)))
                        .anyRequest()
                        .authenticated())
                .securityContext(securityContext -> securityContext.requireExplicitSave(false))
                .csrf(csrf -> csrf.
                        csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .securityContext(securityContext -> securityContext.requireExplicitSave(false));

        return http.build();
    }

    private boolean hasAdminRole(Supplier<Authentication> authentication) {
        if (!(authentication.get() instanceof CustomAuthenticationToken customAuthenticationToken))
            return false;
        return customAuthenticationToken.getRoles().contains("ADMIN");
    }
}
