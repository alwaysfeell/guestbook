package sumdu.edu.ua.persistence.adapter;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.port.CommentRepositoryPort;
import sumdu.edu.ua.persistence.entity.BookEntity;
import sumdu.edu.ua.persistence.entity.CommentEntity;
import sumdu.edu.ua.persistence.entity.UserEntity;
import sumdu.edu.ua.persistence.repository.BookRepository;
import sumdu.edu.ua.persistence.repository.CommentRepository;
import sumdu.edu.ua.persistence.repository.UserRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Component
@Transactional(readOnly = true)
public class JpaCommentRepositoryAdapter implements CommentRepositoryPort {
    private final CommentRepository comments;
    private final BookRepository books;
    private final UserRepository users;

    public JpaCommentRepositoryAdapter(CommentRepository comments, BookRepository books, UserRepository users) {
        this.comments = comments;
        this.books = books;
        this.users = users;
    }

    @Override
    @Transactional
    public Comment add(long bookId, long userId, String text, Instant createdAt) {
        BookEntity book = books.findById(bookId).orElseThrow();
        UserEntity user = users.findById(userId).orElseThrow();
        CommentEntity comment = new CommentEntity(text, createdAt);
        book.addComment(comment);
        user.addComment(comment);
        return EntityMapper.toDomain(comments.save(comment));
    }

    @Override
    public Page<Comment> findByBook(long bookId, PageRequest request) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt", "id");
        Pageable pageable = org.springframework.data.domain.PageRequest.of(request.getPage(), request.getSize(), sort);
        var page = comments.findByBookId(bookId, pageable);
        List<Comment> items = page.getContent().stream().map(EntityMapper::toDomain).toList();
        return new Page<>(items, request, page.getTotalElements());
    }

    @Override
    public List<Comment> findByUser(long userId) {
        return comments.findByUserId(userId).stream()
                .sorted(Comparator.comparing(CommentEntity::getCreatedAt).reversed())
                .map(EntityMapper::toDomain)
                .toList();
    }

    @Override
    public Map<Long, Long> countByUser() {
        Map<Long, Long> result = new TreeMap<>();
        for (Object[] row : comments.countGroupedByUser()) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    @Override
    public Optional<Comment> findById(long bookId, long commentId) {
        return comments.findByIdAndBookId(commentId, bookId).map(EntityMapper::toDomain);
    }

    @Override
    @Transactional
    public boolean update(long bookId, long commentId, String text) {
        Optional<CommentEntity> comment = comments.findByIdAndBookId(commentId, bookId);
        comment.ifPresent(c -> c.setText(text));
        return comment.isPresent();
    }

    @Override
    @Transactional
    public boolean delete(long bookId, long commentId) {
        Optional<CommentEntity> comment = comments.findByIdAndBookId(commentId, bookId);
        comment.ifPresent(comments::delete);
        return comment.isPresent();
    }
}
