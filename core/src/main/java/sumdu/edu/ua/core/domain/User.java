package sumdu.edu.ua.core.domain;

import java.util.Objects;

public class User {
    private final long id;
    private final String username;
    private final String email;
    private final String password;
    private final Role role;
    private final boolean enabled;
    private final String confirmationCode;

    public User(long id, String username, String password, Role role) {
        this(id, username, null, password, role, true, null);
    }

    public User(long id, String username, String email, String password, Role role,
                boolean enabled, String confirmationCode) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.enabled = enabled;
        this.confirmationCode = confirmationCode;
    }

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public String getConfirmationCode() { return confirmationCode; }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public User confirmed() {
        return new User(id, username, email, password, role, true, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return id == other.id && Objects.equals(username, other.username) && role == other.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, role);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role + ", enabled=" + enabled + "}";
    }
}
