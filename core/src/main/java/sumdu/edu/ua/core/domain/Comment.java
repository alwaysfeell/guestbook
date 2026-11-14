package sumdu.edu.ua.core.domain;

import java.time.Instant;
import java.util.Objects;

public class Comment {
    private final long id;
    private final long bookId;
    private final String author;
    private final String text;
    private final Instant createdAt;

    public Comment(long id, long bookId, String author, String text, Instant createdAt) {
        this.id = id;
        this.bookId = bookId;
        this.author = author;
        this.text = text;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getBookId() { return bookId; }
    public String getAuthor() { return author; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }

    public Comment withText(String newText) {
        return new Comment(id, bookId, author, newText, createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Comment)) return false;
        Comment other = (Comment) o;
        return id == other.id && bookId == other.bookId && Objects.equals(author, other.author)
                && Objects.equals(text, other.text) && Objects.equals(createdAt, other.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bookId, author, text, createdAt);
    }

    @Override
    public String toString() {
        return "Comment{id=" + id + ", bookId=" + bookId + ", author='" + author
                + "', length=" + (text == null ? 0 : text.length()) + ", createdAt=" + createdAt + "}";
    }
}
