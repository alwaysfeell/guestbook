package sumdu.edu.ua.web.dto;

import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.Page;

public record BookDetails(Book book, Page<Comment> comments) {
}
