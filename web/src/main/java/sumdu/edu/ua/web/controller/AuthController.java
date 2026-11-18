package sumdu.edu.ua.web.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.exception.ConflictException;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.exception.ValidationException;
import sumdu.edu.ua.core.service.UserService;
import sumdu.edu.ua.web.form.RegisterForm;
import sumdu.edu.ua.web.mail.MailService;

@Controller
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService users;
    private final MailService mailService;

    public AuthController(UserService users, MailService mailService) {
        this.users = users;
        this.mailService = mailService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        return isLoggedIn(authentication) ? "redirect:/books" : "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Authentication authentication, Model model) {
        if (isLoggedIn(authentication)) {
            return "redirect:/books";
        }
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult result,
                           Model model) {
        if (!result.hasFieldErrors("password") && !result.hasFieldErrors("confirmPassword")
                && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "register.passwordMismatch");
        }
        if (!result.hasFieldErrors("username") && users.usernameTaken(form.getUsername())) {
            result.rejectValue("username", "register.usernameTaken");
        }
        if (!result.hasFieldErrors("email") && users.emailTaken(form.getEmail())) {
            result.rejectValue("email", "register.emailTaken");
        }
        if (result.hasErrors()) {
            return "auth/register";
        }

        User user;
        try {
            user = users.register(form.getUsername(), form.getEmail(), form.getPassword());
        } catch (ValidationException | ConflictException e) {
            result.reject("register.rejected", new Object[]{e.getMessage()}, e.getMessage());
            return "auth/register";
        }
        log.info("User registered: username='{}', email='{}', role={}, enabled={}",
                user.getUsername(), user.getEmail(), user.getRole(), user.isEnabled());

        boolean sent = true;
        try {
            mailService.sendConfirmationEmail(user);
        } catch (RuntimeException e) {
            sent = false;
            log.warn("Confirmation e-mail for '{}' was not sent. Confirmation link: {}",
                    user.getUsername(), mailService.confirmationLink(user));
        }
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("mailSent", sent);
        return "auth/register-success";
    }

    @GetMapping("/confirm")
    public ModelAndView confirm(@RequestParam(required = false) String code) {
        try {
            User user = users.confirm(code);
            log.info("Account confirmed: username='{}', role={}", user.getUsername(), user.getRole());
            return new ModelAndView("redirect:/login?confirmed");
        } catch (ValidationException | NotFoundException e) {
            log.warn("Account confirmation failed: {}", e.getMessage());
            return new ModelAndView("auth/confirm-error", HttpStatus.BAD_REQUEST);
        }
    }

    private static boolean isLoggedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
