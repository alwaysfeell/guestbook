package sumdu.edu.ua.core.port;

import sumdu.edu.ua.core.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    List<User> findAll();

    Optional<User> findById(long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByConfirmationCode(String code);

    User save(User user);
}
