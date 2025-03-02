package gg.nya.imagehosting.beans;

import gg.nya.imagehosting.models.User;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.springframework.stereotype.Component;

import gg.nya.imagehosting.models.Role;
import gg.nya.imagehosting.security.UserSession;
import gg.nya.imagehosting.services.AuthenticationService;

import java.util.Optional;

@Component("loginBean")
public class LoginBean {
    private String username;
    private String password;

    private final AuthenticationService authenticationService;
    private final UserSession userSession;

    public LoginBean(AuthenticationService authenticationService, UserSession userSession) {
        this.authenticationService = authenticationService;
        this.userSession = userSession;
    }

    public String login() {
        var user = authenticationService.authenticate(username, password);
        return handleLoginAttemptInternal(user);
    }


    public String signUp() {
        var user = authenticationService.signUp(username, password);
        return handleLoginAttemptInternal(user);
    }

    public String logout() {
        if (userSession.isAuthenticated()) {
            userSession.logout();
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

    private String handleLoginAttemptInternal(Optional<User> user) {
        if(user.isEmpty()) {
            return handleUnsuccessfulLoginAttemptInternal();
        }

        userSession.login(user.get().getId(), user.get().getUsername(),
                user.get().getRoles().stream().map(Role::getRole).toList());
        return "logintest.xhtml?faces-redirect=true";
    }

    private String handleUnsuccessfulLoginAttemptInternal() {
        this.username = "";
        this.password = "";
        userSession.logout();

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Invalid credentials!", null));

        return "index.xhtml?faces-redirect=true";
    }
}
