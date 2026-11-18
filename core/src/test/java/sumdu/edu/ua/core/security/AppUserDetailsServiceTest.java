package sumdu.edu.ua.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import sumdu.edu.ua.core.domain.Role;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.service.InMemoryUserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppUserDetailsServiceTest {
    private final AppUserDetailsService service = new AppUserDetailsService(new InMemoryUserRepository(
            new User(1, "admin", "admin@bookapp.local", "$2a$10$admin", Role.ADMIN, true, null),
            new User(2, "taras", "taras@example.com", "$2a$10$taras", Role.USER, false, "code-1")));

    @Test
    void loadsUserWithRoleAuthority() {
        UserDetails admin = service.loadUserByUsername("admin");
        assertEquals("admin", admin.getUsername());
        assertEquals("$2a$10$admin", admin.getPassword());
        assertEquals("ROLE_ADMIN", admin.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).findFirst().orElseThrow());
        assertTrue(admin.isEnabled());
    }

    @Test
    void notConfirmedUserIsDisabled() {
        UserDetails taras = service.loadUserByUsername("taras");
        assertFalse(taras.isEnabled());
        assertEquals("ROLE_USER", taras.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void unknownUserThrowsUsernameNotFound() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nobody"));
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername(null));
    }
}
