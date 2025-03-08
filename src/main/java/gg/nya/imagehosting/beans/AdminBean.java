package gg.nya.imagehosting.beans;

import gg.nya.imagehosting.config.ApplicationContextProvider;
import gg.nya.imagehosting.models.Role;
import gg.nya.imagehosting.models.User;
import gg.nya.imagehosting.services.RoleService;
import gg.nya.imagehosting.services.UserService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("adminBean")
@Scope("view")
public class AdminBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<User> users;
    private final List<Role> roles;

    private final Map<User, List<String>> currentUserRoles;
    private Map<User, String> selectedRolesToAdd;
    private Map<User, String> selectedRolesToRemove;

    private transient UserService userService;
    private transient RoleService roleService;

    private static final Logger log = LoggerFactory.getLogger(AdminBean.class);

    private long totalUserCount;
    private int page = 0;

    @Autowired
    public AdminBean(UserService userService, RoleService roleService) {
        this.users = new ArrayList<>();
        this.roles = new ArrayList<>();
        this.selectedRolesToAdd = new HashMap<>();
        this.selectedRolesToRemove = new HashMap<>();
        this.currentUserRoles = new HashMap<>();
        this.userService = userService;
        this.roleService = roleService;
    }

    @PostConstruct
    public void init() {
        this.totalUserCount = userService.getUserCount();
        this.roles.addAll(roleService.getRoles());
        this.loadMoreUsers();
    }

    /**
     * Load a batch of up to 10 users from the database, if there are any left.
     */
    public void loadMoreUsers() {
        if (users.size() >= totalUserCount) {
            log.warn("loadMoreUsers, user requested more users, but there are no more users to load");
            return;
        }
        log.debug("loadMoreUsers, user requested more users, loading page: {}", page);
        List<User> newUsers = userService.getUsers(page, 10);
        users.addAll(newUsers);
        this.setUpSelectedRoles();
        page++;
    }

    /**
     * Sets up the initial role selections for each user.
     */
    private void setUpSelectedRoles() {
        for (User user : users) {
            if (this.currentUserRoles.containsKey(user)) {
                continue;
            }
            this.currentUserRoles.put(user, user.getRoles().stream().map(Role::getRole).toList());

            for (Role role : this.roles) {
                if (this.currentUserRoles.get(user).contains(role.getRole())) {
                    this.selectedRolesToRemove.put(user, role.getRole());
                } else {
                    this.selectedRolesToAdd.put(user, role.getRole());
                }
            }
        }
    }

    public List<User> getUsers() {
        return users;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public Map<User, String> getSelectedRolesToAdd() {
        return selectedRolesToAdd;
    }

    public void setSelectedRolesToAdd(Map<User, String> selectedRolesToAdd) {
        this.selectedRolesToAdd = selectedRolesToAdd;
    }

    public Map<User, String> getSelectedRolesToRemove() {
        return selectedRolesToRemove;
    }

    public void setSelectedRolesToRemove(Map<User, String> selectedRolesToRemove) {
        this.selectedRolesToRemove = selectedRolesToRemove;
    }

    public List<String> getCurrentUserRoles(User user) {
        return currentUserRoles.get(user);
    }

    public List<String> getAvailableUserRoles(User user) {
        return roles.stream().map(Role::getRole).filter(role -> !currentUserRoles.get(user).contains(role)).toList();
    }

    public void addRole(User user) {
        log.debug("addRole, adding role {} to user {}", selectedRolesToAdd.get(user), user.getUsername());
    }

    public void removeRole(User user) {
        log.debug("removeRole, removing role {} from user {}", selectedRolesToRemove.get(user), user.getUsername());
    }

    public long getTotalUserCount() {
        return totalUserCount;
    }

    public boolean getHasMoreUsers() {
        return users.size() < getTotalUserCount();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Retrieve the Spring application context (using a helper or static context holder)
        ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
        this.userService = ctx.getBean(UserService.class);
        this.roleService = ctx.getBean(RoleService.class);
    }
}
