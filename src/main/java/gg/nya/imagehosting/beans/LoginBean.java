package gg.nya.imagehosting.beans;

import gg.nya.imagehosting.config.ApplicationContextProvider;
import gg.nya.imagehosting.models.User;
import gg.nya.imagehosting.services.AuthenticationService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

@Component("loginBean")
@Scope("view")
public class LoginBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;

    private static final Logger log = LoggerFactory.getLogger(LoginBean.class);

    private transient AuthenticationService authenticationService;

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

     @Serial
     private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         in.defaultReadObject();
         // Retrieve the Spring application context (using a helper or static context holder)
         ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
         this.authenticationService = ctx.getBean(AuthenticationService.class);
     }
}
