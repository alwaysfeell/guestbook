package sumdu.edu.ua.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.web.dto.NewCommentRequest;
import sumdu.edu.ua.web.dto.UpdateCommentRequest;

@RestController
@RequestMapping("/comments")
public class CommentsController {
    private static final Logger log = LoggerFactory.getLogger(CommentsController.class);

    private final CommentService comments;

    public CommentsController(CommentService comments) {
        this.comments = comments;
    }

    @GetMapping
    public ResponseEntity<Page<Comment>> getComments(
            @RequestParam long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${app.catalog.default-page-size:10}") int size) {
        return ResponseEntity.ok(comments.list(bookId, page, size));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Comment> addComment(@RequestBody NewCommentRequest request) {
        Comment c = comments.add(request.bookId(), request.author(), request.text());
        log.info("Comment created: id={}, bookId={}, author='{}', length={}",
                c.getId(), c.getBookId(), c.getAuthor(), c.getText().length());
        return ResponseEntity.status(HttpStatus.CREATED).body(c);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Comment> updateComment(@PathVariable long id,
                                                 @RequestParam long bookId,
                                                 @RequestBody UpdateCommentRequest request) {
        Comment c = comments.update(bookId, id, request.text());
        log.info("Comment updated: id={}, bookId={}, length={}", c.getId(), c.getBookId(), c.getText().length());
        return ResponseEntity.ok(c);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable long id, @RequestParam long bookId) {
        Comment c = comments.delete(bookId, id);
        log.info("Comment deleted: id={}, bookId={}, author='{}'", c.getId(), c.getBookId(), c.getAuthor());
        return ResponseEntity.noContent().build();
    }
}
