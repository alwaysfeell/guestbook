package sumdu.edu.ua.core.service;

import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import sumdu.edu.ua.core.domain.Role;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.exception.ConflictException;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.exception.ValidationException;
import sumdu.edu.ua.core.port.UserRepositoryPort;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Transactional
public class UserService {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_.-]{3,64}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final UserRepositoryPort repo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepositoryPort repo) {
        this(repo, new BCryptPasswordEncoder());
    }

    public UserService(UserRepositoryPort repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return repo.findAll();
    }

    public User findById(long id) {
        BookService.requirePositiveId(id, "user id");
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id=" + id + " not found"));
    }

    public User findByUsername(String username) {
        String name = username == null ? "" : username.trim();
        if (name.isEmpty()) {
            throw new ValidationException("username is required");
        }
        return repo.findByUsername(name)
                .orElseThrow(() -> new NotFoundException("User '" + name + "' not found"));
    }

    public boolean usernameTaken(String username) {
        return username != null && repo.findByUsername(username.trim()).isPresent();
    }

    public boolean emailTaken(String email) {
        return email != null && repo.findByEmail(normalizeEmail(email)).isPresent();
    }

    public User register(String username, String email, String rawPassword) {
        String name = username == null ? "" : username.trim();
        String mail = normalizeEmail(email);
        if (!USERNAME.matcher(name).matches()) {
            throw new ValidationException("username must be 3-64 characters: latin letters, digits, '.', '_' or '-'");
        }
        if (mail.length() > 128 || !EMAIL.matcher(mail).matches()) {
            throw new ValidationException("email is invalid");
        }
        if (rawPassword == null || rawPassword.length() < 6 || rawPassword.length() > 72) {
            throw new ValidationException("password must be 6-72 characters");
        }
        if (repo.findByUsername(name).isPresent()) {
            throw new ConflictException("Username '" + name + "' is already taken");
        }
        if (repo.findByEmail(mail).isPresent()) {
            throw new ConflictException("Email '" + mail + "' is already registered");
        }
        String code = UUID.randomUUID().toString();
        User user = new User(0, name, mail, passwordEncoder.encode(rawPassword), Role.USER, false, code);
        return repo.save(user);
    }

    public User confirm(String code) {
        String c = code == null ? "" : code.trim();
        if (c.isEmpty()) {
            throw new ValidationException("confirmation code is required");
        }
        User user = repo.findByConfirmationCode(c)
                .orElseThrow(() -> new NotFoundException("Confirmation code is invalid or already used"));
        return repo.save(user.confirmed());
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
