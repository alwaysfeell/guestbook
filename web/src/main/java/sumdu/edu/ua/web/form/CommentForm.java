package sumdu.edu.ua.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentForm {

    @NotBlank
    @Size(max = 64)
    private String author;

    @NotBlank
    @Size(max = 1000)
    private String text;

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
