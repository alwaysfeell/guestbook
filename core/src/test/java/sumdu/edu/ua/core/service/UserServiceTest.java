package sumdu.edu.ua.core.service;

import org.junit.jupiter.api.Test;
import sumdu.edu.ua.core.domain.Role;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.exception.ValidationException;
import sumdu.edu.ua.core.port.UserRepositoryPort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceTest {
    private static final List<User> USERS = List.of(
            new User(1, "admin", "x", Role.ADMIN),
            new User(2, "olena", "y", Role.USER));

    private final UserService service = new UserService(new UserRepositoryPort() {
        public List<User> findAll() { return USERS; }
        public Optional<User> findById(long id) { return USERS.stream().filter(u -> u.getId() == id).findFirst(); }
        public Optional<User> findByUsername(String username) {
            return USERS.stream().filter(u -> u.getUsername().equals(username)).findFirst();
        }
        public User save(User user) { throw new UnsupportedOperationException(); }
    });

    @Test
    void findsUsers() {
        assertEquals(2, service.findAll().size());
        assertEquals("olena", service.findById(2).getUsername());
        assertEquals(Role.ADMIN, service.findByUsername(" admin ").getRole());
    }

    @Test
    void missingUserGivesNotFound() {
        NotFoundException e = assertThrows(NotFoundException.class, () -> service.findById(42));
        assertEquals("User with id=42 not found", e.getMessage());
        assertThrows(NotFoundException.class, () -> service.findByUsername("nobody"));
    }

    @Test
    void validatesArguments() {
        assertThrows(ValidationException.class, () -> service.findById(0));
        assertThrows(ValidationException.class, () -> service.findByUsername("  "));
    }
}
