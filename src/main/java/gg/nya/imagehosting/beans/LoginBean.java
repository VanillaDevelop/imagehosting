package gg.nya.imagehosting.beans;

import gg.nya.imagehosting.models.User;
import gg.nya.imagehosting.services.AuthenticationService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component("loginBean")
public class LoginBean {
    private String username;
    private String password;

    private static final Logger log = LoggerFactory.getLogger(LoginBean.class);

    private final AuthenticationService authenticationService;

    public LoginBean(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public String login() {
        var user = authenticationService.authenticate(username, password);
        return handleLoginAttemptInternal(user);
    }


    public String signUp() {
        var user = authenticationService.signUp(username, password);
        return handleSignUpAttemptInternal(user);
    }

    public String logout() {
        if (authenticationService.isCurrentUserAuthenticated()) {
            authenticationService.logout();
        }
        return "index.xhtml?faces-redirect=true";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void redirectIfAuthenticated() throws IOException {
        if (authenticationService.isCurrentUserAuthenticated()) {
            FacesContext.getCurrentInstance().getExternalContext().redirect("home.xhtml?faces-redirect=true");
        }
    }

    private String handleSignUpAttemptInternal(Optional<User> user) {
        if (user.isEmpty()) {
            return handleUnsuccessfulSignUpAttemptInternal();
        }
        log.info("handleSignUpAttemptInternal, sign up successful for user: {}", user.get().getUsername());
        return "home.xhtml?faces-redirect=true";
    }

    private String handleLoginAttemptInternal(Optional<User> user) {
        if (user.isEmpty()) {
            return handleUnsuccessfulLoginAttemptInternal();
        }
        log.info("handleLoginAttemptInternal, login successful for user: {}", user.get().getUsername());
        return "home.xhtml?faces-redirect=true";
    }

    private String handleUnsuccessfulLoginAttemptInternal() {
        log.info("handleUnsuccessfulLoginAttemptInternal, login unsuccessful for user: {}", username);
        this.username = "";
        this.password = "";
        authenticationService.logout();

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Invalid credentials!", null));

        return null;
    }

    private String handleUnsuccessfulSignUpAttemptInternal() {
        log.info("handleUnsuccessfulSignUpAttemptInternal, sign up unsuccessful for user: {}", username);
        this.username = "";
        this.password = "";
        authenticationService.logout();

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Username already exists!", null));

        return null;
    }
}
