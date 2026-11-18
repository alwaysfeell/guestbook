package sumdu.edu.ua.core.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import sumdu.edu.ua.core.domain.Role;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.exception.ConflictException;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.exception.ValidationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final InMemoryUserRepository repo = new InMemoryUserRepository(
            new User(1, "admin", "admin@bookapp.local", "x", Role.ADMIN, true, null),
            new User(2, "olena", "olena@bookapp.local", "y", Role.USER, true, null));
    private final UserService service = new UserService(repo, encoder);

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

    @Test
    void registerHashesPasswordAndCreatesDisabledUser() {
        User user = service.register(" taras ", " Taras@Example.COM ", "secret1");

        assertTrue(user.getId() > 0);
        assertEquals("taras", user.getUsername());
        assertEquals("taras@example.com", user.getEmail());
        assertEquals(Role.USER, user.getRole());
        assertFalse(user.isEnabled());
        assertNotNull(user.getConfirmationCode());
        assertNotEquals("secret1", user.getPassword());
        assertTrue(user.getPassword().startsWith("$2a$"));
        assertTrue(encoder.matches("secret1", user.getPassword()));
    }

    @Test
    void registerRejectsDuplicatesAndInvalidData() {
        assertThrows(ConflictException.class, () -> service.register("olena", "new@example.com", "secret1"));
        assertThrows(ConflictException.class, () -> service.register("olena2", "OLENA@bookapp.local", "secret1"));
        assertThrows(ValidationException.class, () -> service.register("ab", "a@b.com", "secret1"));
        assertThrows(ValidationException.class, () -> service.register("with space", "a@b.com", "secret1"));
        assertThrows(ValidationException.class, () -> service.register("taras", "not-an-email", "secret1"));
        assertThrows(ValidationException.class, () -> service.register("taras", "a@b.com", "12345"));
        assertTrue(service.usernameTaken("olena"));
        assertTrue(service.emailTaken(" Olena@BookApp.local "));
        assertFalse(service.usernameTaken("taras"));
    }

    @Test
    void confirmEnablesAccountOnlyOnce() {
        User user = service.register("taras", "taras@example.com", "secret1");

        User confirmed = service.confirm(user.getConfirmationCode());
        assertTrue(confirmed.isEnabled());
        assertNull(confirmed.getConfirmationCode());
        assertTrue(service.findByUsername("taras").isEnabled());

        assertThrows(NotFoundException.class, () -> service.confirm(user.getConfirmationCode()));
        assertThrows(ValidationException.class, () -> service.confirm(" "));
    }
}
