package sumdu.edu.ua.web.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sumdu.edu.ua.web.http.HttpSupport;

import java.io.IOException;

public class ErrorPageServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(ErrorPageServlet.class);

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object code = req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object uri = req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object msg = req.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        int status = code instanceof Integer ? (Integer) code : HttpServletResponse.SC_NOT_FOUND;
        String path = uri != null ? uri.toString() : req.getRequestURI();
        String message = msg != null && !msg.toString().isBlank() ? msg.toString() : "Request failed";

        if (status >= 500) {
            log.error("{} {} -> {}: {}", req.getMethod(), path, status, message);
        } else {
            log.warn("{} {} -> {}: {}", req.getMethod(), path, status, message);
        }
        HttpSupport.writeJson(resp, status, new ApiError(status, message, path));
    }
}
