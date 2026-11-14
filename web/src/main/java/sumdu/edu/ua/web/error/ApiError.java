package sumdu.edu.ua.web.error;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public class ApiError {
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final Instant timestamp = Instant.now();

    public ApiError(int status, String message, String path) {
        this.status = status;
        this.error = reason(status);
        this.message = message;
        this.path = path;
    }

    public ApiError(HttpStatus status, String message, String path) {
        this(status.value(), message, path);
    }

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public Instant getTimestamp() { return timestamp; }

    private static String reason(int status) {
        HttpStatus known = HttpStatus.resolve(status);
        if (known != null) {
            return known.getReasonPhrase();
        }
        return status >= 500 ? "Server Error" : "Client Error";
    }
}
