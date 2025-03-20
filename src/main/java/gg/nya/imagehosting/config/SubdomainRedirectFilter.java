package gg.nya.imagehosting.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SubdomainRedirectFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String serverName = httpRequest.getServerName();
        String requestURI = httpRequest.getRequestURI();
        String queryString = httpRequest.getQueryString();

        // Skip redirect for API paths
        if (requestURI.startsWith("/i/")) {
            chain.doFilter(request, response);
            return;
        }

        if (serverName.contains(".")) {
            String protocol = request.isSecure() ? "https" : "http";
            String serverNameWithoutSubdomain = serverName.substring(serverName.indexOf(".") + 1);
            String port = request.getServerPort() == 80 ? "" : ":" + request.getServerPort();
            String redirectUrl = protocol + "://" + serverNameWithoutSubdomain + port + requestURI + (queryString != null ? "?" + queryString : "");
            httpResponse.sendRedirect(redirectUrl);
            return;
        }

        chain.doFilter(request, response);
    }
}
