package sumdu.edu.ua.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sumdu.edu.ua.persistence.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);
}
