package app.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CommentsServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CommentsServlet.class);

    private final CommentDao dao = new CommentDao();
    private final ObjectMapper om = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        try {
            var comments = dao.list();
            om.writeValue(resp.getWriter(), comments);
        } catch (Exception e) {
            log.error("Failed to load comments", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");

        String author = trim(req.getParameter("author"));
        String text = trim(req.getParameter("text"));

        if (author.isEmpty() || author.length() > 64) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "author is required (max 64 chars)");
            return;
        }
        if (text.isEmpty() || text.length() > 1000) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "text is required (max 1000 chars)");
            return;
        }

        try {
            long id = dao.add(author, text);
            log.info("New comment: id={}, author='{}', length={}", id, author, text.length());
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (Exception e) {
            log.error("Failed to save comment", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB error");
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
