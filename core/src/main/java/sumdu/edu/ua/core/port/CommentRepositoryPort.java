package sumdu.edu.ua.core.port;

import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;

import java.time.Instant;
import java.util.Optional;

public interface CommentRepositoryPort {
    Comment add(long bookId, String author, String text);

    Page<Comment> list(long bookId, String author, Instant since, PageRequest request);

    Optional<Comment> findById(long bookId, long commentId);

    boolean delete(long bookId, long commentId);
}
