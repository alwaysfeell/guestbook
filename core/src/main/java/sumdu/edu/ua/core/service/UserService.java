package sumdu.edu.ua.core.service;

import jakarta.transaction.Transactional;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.exception.ValidationException;
import sumdu.edu.ua.core.port.UserRepositoryPort;

import java.util.List;

@Transactional
public class UserService {
    private final UserRepositoryPort repo;

    public UserService(UserRepositoryPort repo) {
        this.repo = repo;
    }

    public List<User> findAll() {
        return repo.findAll();
    }

    public User findById(long id) {
        BookService.requirePositiveId(id, "user id");
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id=" + id + " not found"));
    }

    public User findByUsername(String username) {
        String name = username == null ? "" : username.trim();
        if (name.isEmpty()) {
            throw new ValidationException("username is required");
        }
        return repo.findByUsername(name)
                .orElseThrow(() -> new NotFoundException("User '" + name + "' not found"));
    }
}
