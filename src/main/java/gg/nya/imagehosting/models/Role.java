package gg.nya.imagehosting.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Entity(name = "roles")
public class Role implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role")
    @NotNull
    private String role;

    @ManyToMany(mappedBy = "roles")
    private List<User> users;

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Role other = (Role) obj;
        return role.equals(other.role);
    }

    @Override
    public int hashCode() {
        return role.hashCode();
    }
}
