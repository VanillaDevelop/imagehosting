package gg.nya.imagehosting.beans;

import gg.nya.imagehosting.models.Role;
import gg.nya.imagehosting.models.User;
import gg.nya.imagehosting.services.RoleService;
import gg.nya.imagehosting.services.UserService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("adminBean")
@Scope("view")
public class AdminBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<User> users;
    private int page = 0;
    private transient List<Role> roles;
    private transient Map<User, String> selectedRoles;

    private final transient UserService userService;
    private final transient RoleService roleService;

    private static final Logger log = LoggerFactory.getLogger(AdminBean.class);

    public AdminBean(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
        this.users = new ArrayList<>();
        this.roles = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
        this.roles = roleService.getRoles();
        this.loadMoreUsers();
        this.setUpSelectedRoles();
    }

    private void setUpSelectedRoles() {
        this.selectedRoles = new HashMap<>();
        for (User user : users) {
            if (this.selectedRoles.containsKey(user)) {
                continue;
            }
            if (!this.getAvailableRoles(user).isEmpty()) {
                this.selectedRoles.put(user, getAvailableRoles(user).getFirst());
            }
        }
    }

    public List<User> getUsers() {
        return users;
    }

    public void loadMoreUsers() {
        if (users.size() >= getTotalUserCount()) {
            log.warn("loadMoreUsers, user requested more users, but there are no more users to load");
            return;
        }
        log.debug("loadMoreUsers, user requested more users, loading page: {}", page);
        List<User> newUsers = userService.getUsers(page, 10);
        users.addAll(newUsers);
        setUpSelectedRoles();
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

    public long getTotalUserCount() {
        return userService.getUserCount();
    }

    public boolean getHasMoreUsers() {
        return users.size() < getTotalUserCount();
    }
}
