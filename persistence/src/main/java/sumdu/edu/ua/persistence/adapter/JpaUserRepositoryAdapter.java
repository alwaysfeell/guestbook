package sumdu.edu.ua.persistence.adapter;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.port.UserRepositoryPort;
import sumdu.edu.ua.persistence.entity.UserEntity;
import sumdu.edu.ua.persistence.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class JpaUserRepositoryAdapter implements UserRepositoryPort {
    private final UserRepository repo;

    public JpaUserRepositoryAdapter(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<User> findAll() {
        return repo.findAll(Sort.by("id")).stream().map(EntityMapper::toDomain).toList();
    }

    @Override
    public Optional<User> findById(long id) {
        return repo.findById(id).map(EntityMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repo.findByUsername(username).map(EntityMapper::toDomain);
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = user.getId() == 0
                ? new UserEntity(user.getUsername(), user.getPassword(), user.getRole())
                : repo.findById(user.getId()).orElseThrow();
        entity.setUsername(user.getUsername());
        entity.setPassword(user.getPassword());
        entity.setRole(user.getRole());
        return EntityMapper.toDomain(repo.save(entity));
    }
}
