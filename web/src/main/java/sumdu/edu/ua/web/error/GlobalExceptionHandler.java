package sumdu.edu.ua.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import sumdu.edu.ua.core.exception.ConflictException;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.exception.ValidationException;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView notFound(NotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView noRoute(NoResourceFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, "No route for " + req.getMethod() + " " + req.getRequestURI(), req);
    }

    @ExceptionHandler(ValidationException.class)
    public ModelAndView validation(ValidationException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(ConflictException.class)
    public ModelAndView conflict(ConflictException ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, ex.getName() + " must be an integer, got '" + ex.getValue() + "'", req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ModelAndView missingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, ex.getParameterName() + " is required", req);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView unexpected(Exception ex, HttpServletRequest req) {
        if (ex instanceof ErrorResponse response) {
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            return error(status, ex.getMessage(), req);
        }
        log.error("{} {} -> 500: {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
        return view(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req);
    }

    private static ModelAndView error(HttpStatus status, String message, HttpServletRequest req) {
        log.warn("{} {} -> {}: {}", req.getMethod(), req.getRequestURI(), status.value(), message);
        return view(status, message, req);
    }

    private static ModelAndView view(HttpStatus status, String message, HttpServletRequest req) {
        ModelAndView mav = new ModelAndView("error", status);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", message);
        mav.addObject("path", req.getRequestURI());
        return mav;
    }
}
