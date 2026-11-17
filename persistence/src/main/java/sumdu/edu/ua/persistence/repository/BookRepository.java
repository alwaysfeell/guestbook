package sumdu.edu.ua.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sumdu.edu.ua.persistence.entity.BookEntity;

public interface BookRepository extends JpaRepository<BookEntity, Long> {

    Page<BookEntity> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(
            String title, String author, Pageable pageable);
}
