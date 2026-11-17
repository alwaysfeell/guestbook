package sumdu.edu.ua.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.core.service.UserService;

@Controller
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final CommentService commentService;

    public UserController(UserService userService, CommentService commentService) {
        this.userService = userService;
        this.commentService = commentService;
    }

    @GetMapping
    public String users(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("commentCounts", commentService.countByUser());
        return "users";
    }

    @GetMapping("/{id}/comments")
    public String userComments(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        model.addAttribute("comments", commentService.getCommentsByUser(id));
        model.addAttribute("username", user.getUsername());
        model.addAttribute("user", user);
        return "user-comments";
    }
}
