package sumdu.edu.ua.web.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.exception.ValidationException;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.web.form.BookForm;

import java.time.Clock;
import java.time.Year;

@Controller
@RequestMapping("/books")
public class BooksController {
    private static final Logger log = LoggerFactory.getLogger(BooksController.class);

    private final BookService books;
    private final Clock clock;

    public BooksController(BookService books, Clock clock) {
        this.books = books;
        this.clock = clock;
    }

    @GetMapping
    public String getAllBooks(@RequestParam(required = false) String q,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "${app.catalog.default-page-size:10}") int size,
                              Model model) {
        String query = q == null || q.isBlank() ? null : q.trim();
        Page<Book> result = books.search(query, page, size, null);
        model.addAttribute("books", result.getItems());
        model.addAttribute("page", result);
        model.addAttribute("q", query);
        return "books";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("bookForm", new BookForm());
        return "book-form";
    }

    @PostMapping("/add")
    public String addBook(@Valid @ModelAttribute("bookForm") BookForm form,
                          BindingResult result,
                          RedirectAttributes redirect) {
        int currentYear = Year.now(clock).getValue();
        if (form.getPubYear() != null && form.getPubYear() > currentYear) {
            result.rejectValue("pubYear", "book.pubYear.future", new Object[]{currentYear}, null);
        }
        if (result.hasErrors()) {
            return "book-form";
        }
        Book saved;
        try {
            saved = books.add(form.getTitle(), form.getAuthor(), form.getPubYear());
        } catch (ValidationException e) {
            result.reject("book.rejected", new Object[]{e.getMessage()}, e.getMessage());
            return "book-form";
        }
        log.info("Book created: id={}, title='{}', author='{}'", saved.getId(), saved.getTitle(), saved.getAuthor());
        redirect.addFlashAttribute("notice", "book.added");
        redirect.addFlashAttribute("noticeArg", saved.getTitle());
        return "redirect:/books";
    }
}
