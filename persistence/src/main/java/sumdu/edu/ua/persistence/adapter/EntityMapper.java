package sumdu.edu.ua.persistence.adapter;

import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.persistence.entity.BookEntity;
import sumdu.edu.ua.persistence.entity.CommentEntity;
import sumdu.edu.ua.persistence.entity.UserEntity;

final class EntityMapper {

    private EntityMapper() {
    }

    static Book toDomain(BookEntity e) {
        return new Book(e.getId(), e.getTitle(), e.getAuthor(), e.getYear());
    }

    static User toDomain(UserEntity e) {
        return new User(e.getId(), e.getUsername(), e.getEmail(), e.getPassword(), e.getRole(),
                e.isEnabled(), e.getConfirmationCode());
    }

    static Comment toDomain(CommentEntity e) {
        return new Comment(e.getId(), toDomain(e.getBook()), toDomain(e.getUser()), e.getText(), e.getCreatedAt());
    }
}
