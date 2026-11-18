package sumdu.edu.ua.web.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ErrorPagesController {

    @RequestMapping("/403")
    public ModelAndView forbidden(HttpServletRequest request) {
        return page("error/403", HttpStatus.FORBIDDEN, request);
    }

    @RequestMapping("/404")
    public ModelAndView notFound(HttpServletRequest request) {
        return page("error/404", HttpStatus.NOT_FOUND, request);
    }

    @GetMapping("/problem")
    public String problem(Model model) {
        if (!model.containsAttribute("problemBack")) {
            model.addAttribute("problemBack", "/books");
        }
        return "error/problem";
    }

    private static ModelAndView page(String view, HttpStatus status, HttpServletRequest request) {
        Object original = request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
        ModelAndView mav = new ModelAndView(view, status);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("path", original != null ? original : request.getRequestURI());
        return mav;
    }
}
