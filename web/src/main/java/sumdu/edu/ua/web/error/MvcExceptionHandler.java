package sumdu.edu.ua.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;
import sumdu.edu.ua.core.exception.ForbiddenWordsException;
import sumdu.edu.ua.web.controller.BooksController;
import sumdu.edu.ua.web.controller.CommentsController;

@ControllerAdvice(assignableTypes = {CommentsController.class, BooksController.class})
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class MvcExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(MvcExceptionHandler.class);

    @ExceptionHandler(ForbiddenWordsException.class)
    public String forbiddenWords(ForbiddenWordsException ex, HttpServletRequest request) {
        log.warn("{} {} rejected, redirect to /problem: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
        flash.put("problemMessage", ex.getMessage());
        flash.put("problemBack", request.getRequestURI());
        return "redirect:/problem";
    }
}
