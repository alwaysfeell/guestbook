package sumdu.edu.ua.web.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.exception.ConflictException;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.web.form.CommentForm;

import java.time.Clock;
import java.time.Instant;

@Controller
@RequestMapping("/books/{bookId}/comments")
public class CommentsController {
    private static final Logger log = LoggerFactory.getLogger(CommentsController.class);
    private static final int PAGE_SIZE = 20;

    private final BookService books;
    private final CommentService comments;
    private final Clock clock;

    public CommentsController(BookService books, CommentService comments, Clock clock) {
        this.books = books;
        this.comments = comments;
        this.clock = clock;
    }

    @GetMapping
    public String list(@PathVariable long bookId, Model model) {
        model.addAttribute("commentForm", new CommentForm());
        fillPage(bookId, model);
        return "book-comments";
    }

    @PostMapping
    public String add(@PathVariable long bookId,
                      @Valid @ModelAttribute("commentForm") CommentForm form,
                      BindingResult result,
                      Model model,
                      RedirectAttributes redirect) {
        if (result.hasErrors()) {
            fillPage(bookId, model);
            return "book-comments";
        }
        Comment c = comments.add(bookId, form.getAuthor(), form.getText());
        log.info("Comment created: id={}, bookId={}, author='{}', length={}",
                c.getId(), c.getBookId(), c.getAuthor(), c.getText().length());
        redirect.addFlashAttribute("notice", "comments.added");
        return "redirect:/books/{bookId}/comments";
    }

    @PostMapping("/{commentId}/delete")
    public String delete(@PathVariable long bookId,
                         @PathVariable long commentId,
                         RedirectAttributes redirect) {
        try {
            Comment c = comments.delete(bookId, commentId);
            log.info("Comment deleted: id={}, bookId={}, author='{}'", c.getId(), c.getBookId(), c.getAuthor());
            redirect.addFlashAttribute("notice", "comments.deleted");
        } catch (ConflictException e) {
            log.warn("Comment {} of book {} was not deleted: {}", commentId, bookId, e.getMessage());
            redirect.addFlashAttribute("alert", "comments.deleteForbidden");
        }
        return "redirect:/books/{bookId}/comments";
    }

    private void fillPage(long bookId, Model model) {
        model.addAttribute("book", books.getById(bookId));
        model.addAttribute("comments", comments.list(bookId, 0, PAGE_SIZE));
        model.addAttribute("deletableAfter", Instant.now(clock).minus(comments.getDeleteWindow()));
    }
}
