package sumdu.edu.ua.web.error;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sumdu.edu.ua.core.exception.ConflictException;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.exception.ValidationException;
import sumdu.edu.ua.web.http.HttpSupport;

import java.io.IOException;

public class ErrorFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ErrorFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        if (req.getCharacterEncoding() == null) {
            req.setCharacterEncoding("UTF-8");
        }
        try {
            chain.doFilter(req, resp);
        } catch (Exception e) {
            handle(req, resp, unwrap(e));
        }
    }

    private void handle(HttpServletRequest req, HttpServletResponse resp, Throwable ex) throws IOException {
        int status = statusFor(ex);
        String path = req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
        String message;
        if (status >= 500) {
            message = "Internal server error";
            log.error("{} {} -> {}: {}", req.getMethod(), path, status, ex.getMessage(), ex);
        } else {
            message = ex.getMessage();
            log.warn("{} {} -> {}: {}", req.getMethod(), path, status, message);
        }
        if (!resp.isCommitted()) {
            resp.reset();
            HttpSupport.writeJson(resp, status, new ApiError(status, message, req.getRequestURI()));
        }
    }

    static int statusFor(Throwable ex) {
        if (ex instanceof ValidationException) return HttpServletResponse.SC_BAD_REQUEST;
        if (ex instanceof NotFoundException) return HttpServletResponse.SC_NOT_FOUND;
        if (ex instanceof ConflictException) return HttpServletResponse.SC_CONFLICT;
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    private static Throwable unwrap(Throwable ex) {
        Throwable t = ex;
        while (t instanceof ServletException && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }
}
