package sumdu.edu.ua.core.domain;

import java.util.Objects;

public class Book {
    private final long id;
    private final String title;
    private final String author;
    private final int pubYear;

    public Book(long id, String title, String author, int pubYear) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.pubYear = pubYear;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getPubYear() { return pubYear; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        Book other = (Book) o;
        return id == other.id && pubYear == other.pubYear
                && Objects.equals(title, other.title) && Objects.equals(author, other.author);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, author, pubYear);
    }

    @Override
    public String toString() {
        return "Book{id=" + id + ", title='" + title + "', author='" + author + "', pubYear=" + pubYear + "}";
    }
}
