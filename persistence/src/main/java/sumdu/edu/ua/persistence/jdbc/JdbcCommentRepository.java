package sumdu.edu.ua.persistence.jdbc;

import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.port.CommentRepositoryPort;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCommentRepository implements CommentRepositoryPort {
    @Override
    public Comment add(long bookId, String author, String text) {
        Instant now = Instant.now();
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(
                     "insert into comments(book_id, author, text, created_at) values (?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, bookId);
            ps.setString(2, author);
            ps.setString(3, text);
            ps.setTimestamp(4, Timestamp.from(now));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Comment(keys.getLong(1), bookId, author, text, now);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("DB error while inserting comment", e);
        }
    }

    @Override
    public Page<Comment> list(long bookId, String author, Instant since, PageRequest request) {
        StringBuilder where = new StringBuilder(" where book_id = ?");
        if (author != null && !author.isBlank()) {
            where.append(" and author like ?");
        }
        if (since != null) {
            where.append(" and created_at >= ?");
        }
        try (Connection c = Db.get()) {
            List<Comment> items = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "select id, book_id, author, text, created_at from comments" + where
                            + " order by created_at desc, id desc limit ? offset ?")) {
                int i = bind(ps, bookId, author, since);
                ps.setInt(i++, request.getSize());
                ps.setInt(i, request.getOffset());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(map(rs));
                    }
                }
            }
            long total;
            try (PreparedStatement ps = c.prepareStatement("select count(*) from comments" + where)) {
                bind(ps, bookId, author, since);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            return new Page<>(items, request, total);
        } catch (SQLException e) {
            throw new IllegalStateException("DB error while listing comments", e);
        }
    }

    @Override
    public Optional<Comment> findById(long bookId, long commentId) {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(
                     "select id, book_id, author, text, created_at from comments where id = ? and book_id = ?")) {
            ps.setLong(1, commentId);
            ps.setLong(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("DB error while loading comment " + commentId, e);
        }
    }

    @Override
    public boolean delete(long bookId, long commentId) {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("delete from comments where id = ? and book_id = ?")) {
            ps.setLong(1, commentId);
            ps.setLong(2, bookId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("DB error while deleting comment " + commentId, e);
        }
    }

    private static int bind(PreparedStatement ps, long bookId, String author, Instant since) throws SQLException {
        int i = 1;
        ps.setLong(i++, bookId);
        if (author != null && !author.isBlank()) {
            ps.setString(i++, "%" + author + "%");
        }
        if (since != null) {
            ps.setTimestamp(i++, Timestamp.from(since));
        }
        return i;
    }

    private static Comment map(ResultSet rs) throws SQLException {
        return new Comment(
                rs.getLong("id"),
                rs.getLong("book_id"),
                rs.getString("author"),
                rs.getString("text"),
                rs.getTimestamp("created_at").toInstant());
    }
}
