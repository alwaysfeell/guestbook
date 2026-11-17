package sumdu.edu.ua.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sumdu.edu.ua.persistence.entity.CommentEntity;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    @EntityGraph(attributePaths = {"book", "user"})
    List<CommentEntity> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"book", "user"})
    Page<CommentEntity> findByBookId(Long bookId, Pageable pageable);

    @EntityGraph(attributePaths = {"book", "user"})
    Optional<CommentEntity> findByIdAndBookId(Long id, Long bookId);

    @Query("select c.user.id, count(c) from CommentEntity c group by c.user.id")
    List<Object[]> countGroupedByUser();
}
