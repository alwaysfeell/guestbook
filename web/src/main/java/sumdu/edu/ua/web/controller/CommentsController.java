package sumdu.edu.ua.web.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.core.service.UserService;
import sumdu.edu.ua.web.form.CommentForm;

import java.time.Clock;
import java.time.Instant;

@Controller
public class CommentsController {
    private static final Logger log = LoggerFactory.getLogger(CommentsController.class);
    private static final int PAGE_SIZE = 20;

    private final BookService books;
    private final CommentService commentService;
    private final UserService users;
    private final Clock clock;

    public CommentsController(BookService books, CommentService commentService, UserService users, Clock clock) {
        this.books = books;
        this.commentService = commentService;
        this.users = users;
        this.clock = clock;
    }

    @GetMapping("/books/{bookId}/comments")
    public String list(@PathVariable long bookId, Model model) {
        model.addAttribute("commentForm", new CommentForm());
        fillPage(bookId, model);
        return "book-comments";
    }

    @PostMapping("/books/{bookId}/comments")
    public String add(@PathVariable long bookId,
                      @Valid @ModelAttribute("commentForm") CommentForm form,
                      BindingResult result,
                      Authentication authentication,
                      Model model,
                      RedirectAttributes redirect) {
        if (result.hasErrors()) {
            fillPage(bookId, model);
            return "book-comments";
        }
        User author = users.findByUsername(authentication.getName());
        Comment c = commentService.add(bookId, author.getId(), form.getText());
        log.info("Comment created: id={}, bookId={}, user='{}', length={}",
                c.getId(), c.getBookId(), c.getAuthor(), c.getText().length());
        redirect.addFlashAttribute("notice", "comments.added");
        return "redirect:/books/{bookId}/comments";
    }

    @PostMapping("/comments/delete")
    public String delete(@RequestParam(defaultValue = "0") long bookId,
                         @RequestParam(defaultValue = "0") long commentId,
                         @RequestParam(required = false) Instant createdAt,
                         Authentication authentication,
                         RedirectAttributes redirect) {
        commentService.delete(bookId, commentId, createdAt);
        log.info("Comment deleted by '{}': id={}, bookId={}", authentication.getName(), commentId, bookId);
        redirect.addFlashAttribute("notice", "comments.deleted");
        return "redirect:/books/" + bookId + "/comments";
    }

    private void fillPage(long bookId, Model model) {
        model.addAttribute("book", books.findById(bookId));
        model.addAttribute("comments", commentService.list(bookId, 0, PAGE_SIZE));
        model.addAttribute("deletableAfter", Instant.now(clock).minus(commentService.getDeleteWindow()));
    }
}
