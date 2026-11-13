package sumdu.edu.ua.persistence.jdbc;

import org.springframework.stereotype.Repository;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.port.CatalogRepositoryPort;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcBookRepository implements CatalogRepositoryPort {
    private final Db db;

    public JdbcBookRepository(Db db) {
        this.db = db;
    }

    @Override
    public Page<Book> search(String query, PageRequest request) {
        boolean filter = query != null && !query.isBlank();
        String where = filter ? " where lower(title) like ? or lower(author) like ?" : "";
        String order = " order by " + column(request.getSortField())
                + (request.isAscending() ? " asc" : " desc") + ", id asc";

        try (Connection c = db.get()) {
            List<Book> items = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "select id, title, author, pub_year from books" + where + order + " limit ? offset ?")) {
                int i = bindFilter(ps, query, filter);
                ps.setInt(i++, request.getSize());
                ps.setInt(i, request.getOffset());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(map(rs));
                    }
                }
            }
            long total;
            try (PreparedStatement ps = c.prepareStatement("select count(*) from books" + where)) {
                bindFilter(ps, query, filter);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            return new Page<>(items, request, total);
        } catch (SQLException e) {
            throw new IllegalStateException("DB error while searching books", e);
        }
    }

    @Override
    public Optional<Book> findById(long id) {
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement(
                     "select id, title, author, pub_year from books where id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("DB error while loading book " + id, e);
        }
    }

    @Override
    public Book add(String title, String author, int pubYear) {
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement(
                     "insert into books(title, author, pub_year) values (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setInt(3, pubYear);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Book(keys.getLong(1), title, author, pubYear);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("DB error while inserting book", e);
        }
    }

    private static String column(String sortField) {
        switch (sortField) {
            case "title": return "title";
            case "author": return "author";
            case "pubYear": return "pub_year";
            default: return "id";
        }
    }

    private static int bindFilter(PreparedStatement ps, String query, boolean filter) throws SQLException {
        int i = 1;
        if (filter) {
            String pattern = "%" + query.toLowerCase() + "%";
            ps.setString(i++, pattern);
            ps.setString(i++, pattern);
        }
        return i;
    }

    private static Book map(ResultSet rs) throws SQLException {
        return new Book(rs.getLong("id"), rs.getString("title"), rs.getString("author"), rs.getInt("pub_year"));
    }
}
