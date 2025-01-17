package gg.nya.imagehosting.config;

import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;

import gg.nya.imagehosting.security.CustomAuthenticationToken;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/javax.faces.resource/**", "/index.xhtml", "/")
                        .permitAll()
                        .requestMatchers("/admin.xhtml")
                        .access((authentication, context) -> new AuthorizationDecision(hasAdminRole(authentication)))
                        .anyRequest()
                        .authenticated())
                .securityContext(securityContext -> securityContext.requireExplicitSave(false))
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    private boolean hasAdminRole(Supplier<Authentication> authentication) {
        if (!(authentication.get() instanceof CustomAuthenticationToken))
            return false;
        CustomAuthenticationToken customAuthenticationToken = (CustomAuthenticationToken) authentication.get();
        return customAuthenticationToken.getRoles().contains("ADMIN");
    }
}
