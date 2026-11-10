package sumdu.edu.ua.web.javalin;

public record CommentRequest(Long bookId, String author, String text) {
}
