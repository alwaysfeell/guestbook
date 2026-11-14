package sumdu.edu.ua.web.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements ErrorController {
    private static final Logger log = LoggerFactory.getLogger(ApiErrorController.class);

    @RequestMapping("/error")
    public ResponseEntity<ApiError> error(HttpServletRequest req) {
        Object code = req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object uri = req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object msg = req.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        int status = code instanceof Integer i ? i : 404;
        String path = uri != null ? uri.toString() : req.getRequestURI();
        String message = msg != null && !msg.toString().isBlank() ? msg.toString() : "Request failed";

        if (status >= 500) {
            log.error("{} {} -> {}: {}", req.getMethod(), path, status, message);
        } else {
            log.warn("{} {} -> {}: {}", req.getMethod(), path, status, message);
        }
        return ResponseEntity.status(status).body(new ApiError(status, message, path));
    }
}
