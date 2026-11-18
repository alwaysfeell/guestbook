package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.exception.CommentTooOldException;
import sumdu.edu.ua.core.exception.InvalidCommentDeleteException;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.web.aop.CommentServiceLoggingAspect;
import sumdu.edu.ua.web.aop.ServiceTimingAspect;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:aop-logging;DB_CLOSE_DELAY=-1")
@ExtendWith(OutputCaptureExtension.class)
@WithMockUser(username = "admin", roles = "ADMIN")
class AopLoggingTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private CommentService comments;

    @Autowired
    private BookService books;

    @Test
    void aspectsAreRegisteredAndServicesAreProxied() {
        assertThat(context.getBean(CommentServiceLoggingAspect.class)).isNotNull();
        assertThat(context.getBean(ServiceTimingAspect.class)).isNotNull();
        assertThat(AopUtils.isAopProxy(comments)).isTrue();
        assertThat(AopUtils.isAopProxy(books)).isTrue();
    }

    @Test
    void successfulDeleteIsLoggedWithTime(CapturedOutput output) {
        Comment fresh = comments.list(4, 0, 10).getItems().get(0);

        comments.delete(4, fresh.getId(), fresh.getCreatedAt());

        assertThat(output)
                .contains("Calling CommentService.delete(bookId=4, commentId=" + fresh.getId() + ")")
                .containsPattern("CommentService\\.delete\\(bookId=4, commentId=" + fresh.getId()
                        + "\\) finished in \\d+ ms")
                .containsPattern("\\[timing] CommentService\\.delete\\(\\) took [0-9.]+ ms");
        assertThat(comments.list(4, 0, 10).getTotal()).isZero();
    }

    @Test
    void failedDeleteIsLoggedAndExceptionIsRethrown(CapturedOutput output) {
        Instant old = Instant.now().minus(Duration.ofDays(3));

        assertThatThrownBy(() -> comments.delete(11, 4, old)).isInstanceOf(CommentTooOldException.class);
        assertThatThrownBy(() -> comments.delete(-1, 4, old)).isInstanceOf(InvalidCommentDeleteException.class);

        assertThat(output)
                .containsPattern("CommentService\\.delete\\(bookId=11, commentId=4\\) failed in \\d+ ms: ")
                .containsPattern("CommentService\\.delete\\(bookId=-1, commentId=4\\) failed in \\d+ ms: ")
                .containsPattern("\\[timing] CommentService\\.delete\\(\\) took [0-9.]+ ms and threw CommentTooOldException");
    }

    @Test
    void timingAspectCoversAllServiceMethods(CapturedOutput output) {
        books.findById(1);
        comments.list(1, 0, 10);

        assertThat(output)
                .containsPattern("\\[timing] BookService\\.findById\\(\\) took [0-9.]+ ms")
                .containsPattern("\\[timing] CommentService\\.list\\(\\) took [0-9.]+ ms");
    }
}
