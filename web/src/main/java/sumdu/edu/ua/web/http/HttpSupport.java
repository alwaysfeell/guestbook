package sumdu.edu.ua.web.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sumdu.edu.ua.core.exception.ValidationException;

import java.io.IOException;

public final class HttpSupport {
    public static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private HttpSupport() {
    }

    public static void writeJson(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        JSON.writeValue(resp.getWriter(), body);
    }

    public static <T> T bean(ServletContext ctx, Class<T> type) {
        Object bean = ctx.getAttribute(type.getName());
        if (bean == null) {
            throw new IllegalStateException(type.getSimpleName() + " is not initialized");
        }
        return type.cast(bean);
    }

    public static int intParam(HttpServletRequest req, String name, int defaultValue) {
        String value = req.getParameter(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(name + " must be an integer");
        }
    }

    public static Integer optionalInt(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return (value == null || value.isBlank()) ? null : intParam(req, name, 0);
    }

    public static long requiredId(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (value == null || value.isBlank()) {
            throw new ValidationException(name + " is required");
        }
        return parseId(value, name);
    }

    public static long parseId(String value, String name) {
        try {
            long id = Long.parseLong(value.trim());
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
        }
        throw new ValidationException(name + " must be a positive integer");
    }
}
