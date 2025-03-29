package gg.nya.imagehosting.beans;

import gg.nya.imagehosting.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("userDataBean")
public class UserDataBean {
    private final AuthenticationService authenticationService;

    @Autowired
    public UserDataBean(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public String getUsername() {
        return this.authenticationService.getCurrentUsername();
    }

    public boolean hasRole(String role) {
        return this.authenticationService.getCurrentUserRoles().contains(role);
    }
}
