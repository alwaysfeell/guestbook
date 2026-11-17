package sumdu.edu.ua.core.port;

import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CommentRepositoryPort {
    Comment add(long bookId, long userId, String text, Instant createdAt);

    Page<Comment> findByBook(long bookId, PageRequest request);

    List<Comment> findByUser(long userId);

    Map<Long, Long> countByUser();

    Optional<Comment> findById(long bookId, long commentId);

    boolean update(long bookId, long commentId, String text);

    boolean delete(long bookId, long commentId);
}
