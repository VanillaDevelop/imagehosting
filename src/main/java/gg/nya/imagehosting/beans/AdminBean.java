package gg.nya.imagehosting.beans;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import gg.nya.imagehosting.models.Role;
import gg.nya.imagehosting.models.User;
import gg.nya.imagehosting.services.RoleService;
import gg.nya.imagehosting.services.UserService;
import jakarta.annotation.PostConstruct;

@Component("adminBean")
@Scope("view")
public class AdminBean {
    private List<User> users;
    private int page = 0;
    private List<Role> roles;
    private Map<User, String> selectedRoles;

    private final UserService userService;
    private final RoleService roleService;

    private static final Logger log = LoggerFactory.getLogger(AdminBean.class);

    public AdminBean(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @PostConstruct
    public void init() {
        this.loadMoreUsers();
        this.roles = roleService.getRoles();
        this.setUpSelectedRoles();
    }

    private void setUpSelectedRoles() {
        this.selectedRoles = new HashMap<>();
        for(User user : users) {
            if(!this.getAvailableRoles(user).isEmpty()) {
                this.selectedRoles.put(user, getAvailableRoles(user).getFirst());
            }
        }
    }

    public List<User> getUsers() {
        return users;
    }

    public void loadMoreUsers() {
        log.debug("loadMoreUsers, user requested more users, loading page: {}", page);
        users = userService.getUsers(page, 10);
        page++;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public List<String> getAvailableRoles(User user) {
        return roles.stream().filter(role -> !user.getRoles().contains(role)).map(Role::getRole)
                .collect(Collectors.toList());
    }

    public Map<User, String> getSelectedRoles() {
        return selectedRoles;
    }

    public void setSelectedRoles(Map<User, String> selectedRoles) {
        this.selectedRoles = selectedRoles;
    }

    public void addRole(User user) {
        log.debug("addRole, adding role {} to user {}", selectedRoles.get(user), user.getUsername());
    }

    public void removeRole(User user, Role role) {
        log.debug("removeRole, removing role {} from user {}", role.getRole(), user.getUsername());
    }
}
