package sumdu.edu.ua.persistence;

import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.domain.Role;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.persistence.adapter.JpaCatalogRepositoryAdapter;
import sumdu.edu.ua.persistence.adapter.JpaCommentRepositoryAdapter;
import sumdu.edu.ua.persistence.adapter.JpaUserRepositoryAdapter;
import sumdu.edu.ua.persistence.entity.BookEntity;
import sumdu.edu.ua.persistence.entity.CommentEntity;
import sumdu.edu.ua.persistence.entity.UserEntity;
import sumdu.edu.ua.persistence.repository.BookRepository;
import sumdu.edu.ua.persistence.repository.CommentRepository;
import sumdu.edu.ua.persistence.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Import({JpaCatalogRepositoryAdapter.class, JpaUserRepositoryAdapter.class, JpaCommentRepositoryAdapter.class})
class JpaRepositoriesTest {

    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private JpaCatalogRepositoryAdapter catalog;
    @Autowired private JpaUserRepositoryAdapter users;
    @Autowired private JpaCommentRepositoryAdapter comments;
    @Autowired private EntityManager em;

    @Test
    void flywayAppliedMigrationsWithSeedData() {
        List<Object> versions = em.createNativeQuery(
                "select \"version\" from \"flyway_schema_history\" where \"success\" and \"version\" is not null order by \"installed_rank\"")
                .getResultList();
        assertThat(versions).containsExactly("1", "2", "3");
        assertThat(bookRepository.count()).isEqualTo(12);
        assertThat(userRepository.count()).isEqualTo(5);
        assertThat(commentRepository.count()).isEqualTo(12);
    }

    @Test
    void findByUserIdReturnsCommentsWithBooks() {
        UserEntity vladyslav = userRepository.findByUsername("vladyslav").orElseThrow();
        List<CommentEntity> list = commentRepository.findByUserId(vladyslav.getId());
        assertThat(list).hasSize(3);
        assertThat(Hibernate.isInitialized(list.get(0).getBook())).isTrue();
        assertThat(list).extracting(c -> c.getBook().getTitle())
                .containsExactlyInAnyOrder("Clean Code", "Domain-Driven Design", "Effective Java");
    }

    @Test
    void oneToManyCollectionsAreLazy() {
        BookEntity book = bookRepository.findById(12L).orElseThrow();
        assertThat(Hibernate.isInitialized(book.getComments())).isFalse();
        assertThat(book.getComments()).hasSize(2);
        assertThat(book.getComments().get(0).getCreatedAt())
                .isAfter(book.getComments().get(1).getCreatedAt());
    }

    @Test
    void addCommentLinksBookAndUser() {
        User olena = users.findByUsername("olena").orElseThrow();
        Comment saved = comments.add(5, olena.getId(), "Роман про Київ 1920-х", Instant.now());
        em.flush();
        em.clear();

        CommentEntity reloaded = commentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getBook().getTitle()).isEqualTo("Місто");
        assertThat(reloaded.getUser().getUsername()).isEqualTo("olena");
        assertThat(comments.findByUser(olena.getId())).first()
                .extracting(Comment::getText).isEqualTo("Роман про Київ 1920-х");
    }

    @Test
    void deletingBookCascadesToComments() {
        long before = commentRepository.count();
        assertThat(catalog.deleteById(11)).isTrue();
        em.flush();
        assertThat(bookRepository.existsById(11L)).isFalse();
        assertThat(commentRepository.count()).isEqualTo(before - 2);
        assertThat(catalog.deleteById(11)).isFalse();
    }

    @Test
    void savingUserCascadesNewComments() {
        BookEntity book = bookRepository.findById(1L).orElseThrow();
        UserEntity user = new UserEntity("taras", "$2a$10$hash", Role.USER);
        CommentEntity comment = new CommentEntity("Перший відгук нового користувача", Instant.now());
        book.addComment(comment);
        user.addComment(comment);
        userRepository.save(user);
        em.flush();
        em.clear();

        assertThat(commentRepository.findByUserId(user.getId())).hasSize(1);
    }

    @Test
    void catalogAdapterSearchesSortsAndPaginates() {
        Page<Book> page = catalog.search("martin", new PageRequest(0, 1, "pubYear", false));
        assertThat(page.getTotal()).isEqualTo(3);
        assertThat(page.getItems()).extracting(Book::getTitle).containsExactly("Clean Architecture");

        Page<Book> second = catalog.search(null, new PageRequest(1, 5, "id", true));
        assertThat(second.getTotal()).isEqualTo(12);
        assertThat(second.getItems().get(0).getId()).isEqualTo(6);
    }

    @Test
    void catalogAdapterSavesAndUpdatesBooks() {
        Book saved = catalog.save(new Book(0, "Intermezzo", "Михайло Коцюбинський", 1908));
        assertThat(saved.getId()).isPositive();
        catalog.save(new Book(saved.getId(), "Intermezzo", "М. Коцюбинський", 1908));
        em.flush();
        em.clear();
        assertThat(catalog.findById(saved.getId())).get().extracting(Book::getAuthor).isEqualTo("М. Коцюбинський");
        assertThat(catalog.findAll()).hasSize(13);
    }

    @Test
    void commentAdapterPagesUpdatesDeletesAndCounts() {
        Page<Comment> page = comments.findByBook(12, new PageRequest(0, 10));
        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getItems()).extracting(Comment::getAuthor).containsExactly("vladyslav", "admin");

        long id = page.getItems().get(0).getId();
        assertThat(comments.update(12, id, "Оновлений текст")).isTrue();
        assertThat(comments.update(1, id, "чужа книга")).isFalse();
        em.flush();
        em.clear();
        assertThat(comments.findById(12, id)).get().extracting(Comment::getText).isEqualTo("Оновлений текст");

        Map<Long, Long> counts = comments.countByUser();
        assertThat(counts.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(12);

        assertThat(comments.delete(12, id)).isTrue();
        assertThat(comments.findById(12, id)).isEmpty();
    }

    @Test
    void userAdapterMapsRoleAndKeepsUsernameUnique() {
        assertThat(users.findAll()).extracting(User::getUsername)
                .containsExactly("admin", "olena", "andriy", "vladyslav", "iryna");
        assertThat(users.findById(1)).get().extracting(User::getRole).isEqualTo(Role.ADMIN);
        User saved = users.save(new User(0, "maria", "$2a$10$hash", Role.USER));
        assertThat(users.findByUsername("maria")).get().extracting(User::getId).isEqualTo(saved.getId());
    }

    @Test
    void securityMigrationAddsEmailsAndEnablesSeedUsers() {
        assertThat(users.findAll()).allMatch(User::isEnabled)
                .allMatch(u -> u.getConfirmationCode() == null)
                .extracting(User::getEmail)
                .containsExactly("admin@bookapp.local", "olena@bookapp.local", "andriy@bookapp.local",
                        "vladyslav@bookapp.local", "iryna@bookapp.local");
        assertThat(users.findByEmail("vladyslav@bookapp.local")).get()
                .extracting(User::getUsername).isEqualTo("vladyslav");
    }

    @Test
    void registeredUserIsStoredDisabledAndFoundByConfirmationCode() {
        User saved = users.save(new User(0, "taras", "taras@example.com", "$2a$10$hash", Role.USER, false, "code-123"));
        em.flush();
        em.clear();

        User pending = users.findByConfirmationCode("code-123").orElseThrow();
        assertThat(pending.getId()).isEqualTo(saved.getId());
        assertThat(pending.isEnabled()).isFalse();
        assertThat(pending.getRole()).isEqualTo(Role.USER);

        users.save(pending.confirmed());
        em.flush();
        em.clear();
        assertThat(users.findByConfirmationCode("code-123")).isEmpty();
        assertThat(users.findByUsername("taras")).get().extracting(User::isEnabled).isEqualTo(true);
    }

    @Test
    void emailMustBeUnique() {
        UserEntity duplicate = new UserEntity("olena2", "$2a$10$hash", Role.USER);
        duplicate.setEmail("olena@bookapp.local");
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
