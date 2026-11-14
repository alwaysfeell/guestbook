package sumdu.edu.ua.web.dto;

public record NewCommentRequest(long bookId, String author, String text) {
}
