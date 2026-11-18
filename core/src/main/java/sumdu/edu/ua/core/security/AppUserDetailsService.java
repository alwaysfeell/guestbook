package sumdu.edu.ua.core.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.port.UserRepositoryPort;

public class AppUserDetailsService implements UserDetailsService {
    private final UserRepositoryPort users;

    public AppUserDetailsService(UserRepositoryPort users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String name = username == null ? "" : username.trim();
        User user = users.findByUsername(name)
                .orElseThrow(() -> new UsernameNotFoundException("User '" + name + "' not found"));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .disabled(!user.isEnabled())
                .build();
    }
}
