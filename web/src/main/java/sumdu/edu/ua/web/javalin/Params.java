package sumdu.edu.ua.web.javalin;

import io.javalin.http.Context;
import sumdu.edu.ua.core.exception.ValidationException;

final class Params {
    private Params() {
    }

    static long pathId(Context ctx, String name) {
        return parseId(ctx.pathParam(name), name);
    }

    static long requiredQueryId(Context ctx, String name) {
        String value = ctx.queryParam(name);
        if (value == null || value.isBlank()) {
            throw new ValidationException(name + " is required");
        }
        return parseId(value, name);
    }

    static int intQuery(Context ctx, String name, int defaultValue) {
        String value = ctx.queryParam(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(name + " must be an integer");
        }
    }

    static <T> T body(Context ctx, Class<T> type) {
        T value;
        try {
            value = ctx.bodyAsClass(type);
        } catch (Exception e) {
            throw new ValidationException("Request body must be valid JSON");
        }
        if (value == null) {
            throw new ValidationException("Request body is required");
        }
        return value;
    }

    private static long parseId(String value, String name) {
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
