package sumdu.edu.ua.core.service;

import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.port.UserRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InMemoryUserRepository implements UserRepositoryPort {
    private final List<User> data = new ArrayList<>();
    private long nextId = 1;

    public InMemoryUserRepository(User... users) {
        for (User u : users) {
            data.add(u);
            nextId = Math.max(nextId, u.getId() + 1);
        }
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(data);
    }

    @Override
    public Optional<User> findById(long id) {
        return data.stream().filter(u -> u.getId() == id).findFirst();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return data.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return data.stream().filter(u -> Objects.equals(u.getEmail(), email)).findFirst();
    }

    @Override
    public Optional<User> findByConfirmationCode(String code) {
        return data.stream().filter(u -> Objects.equals(u.getConfirmationCode(), code)).findFirst();
    }

    @Override
    public User save(User user) {
        User stored = user.getId() == 0
                ? new User(nextId++, user.getUsername(), user.getEmail(), user.getPassword(), user.getRole(),
                user.isEnabled(), user.getConfirmationCode())
                : user;
        data.removeIf(u -> u.getId() == stored.getId());
        data.add(stored);
        return stored;
    }
}
