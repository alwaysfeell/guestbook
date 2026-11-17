package sumdu.edu.ua.core.domain;

import java.time.Instant;
import java.util.Objects;

public class Comment {
    private final long id;
    private final Book book;
    private final User user;
    private final String text;
    private final Instant createdAt;

    public Comment(long id, Book book, User user, String text, Instant createdAt) {
        this.id = id;
        this.book = book;
        this.user = user;
        this.text = text;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public Book getBook() { return book; }
    public User getUser() { return user; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }

    public long getBookId() {
        return book.getId();
    }

    public long getUserId() {
        return user.getId();
    }

    public String getAuthor() {
        return user.getUsername();
    }

    public Comment withText(String newText) {
        return new Comment(id, book, user, newText, createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Comment)) return false;
        Comment other = (Comment) o;
        return id == other.id && getBookId() == other.getBookId() && getUserId() == other.getUserId()
                && Objects.equals(text, other.text) && Objects.equals(createdAt, other.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, getBookId(), getUserId(), text, createdAt);
    }

    @Override
    public String toString() {
        return "Comment{id=" + id + ", bookId=" + getBookId() + ", user='" + getAuthor()
                + "', length=" + (text == null ? 0 : text.length()) + ", createdAt=" + createdAt + "}";
    }
}
