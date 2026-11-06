package sumdu.edu.ua.web.error;

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

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public Instant getTimestamp() { return timestamp; }

    private static String reason(int status) {
        switch (status) {
            case 400: return "Bad Request";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 409: return "Conflict";
            case 415: return "Unsupported Media Type";
            case 500: return "Internal Server Error";
            default: return status >= 500 ? "Server Error" : "Client Error";
        }
    }
}
