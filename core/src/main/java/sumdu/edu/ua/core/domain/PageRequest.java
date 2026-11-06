package sumdu.edu.ua.core.domain;

import sumdu.edu.ua.core.exception.ValidationException;

public class PageRequest {
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    private final int page;
    private final int size;
    private final String sortField;
    private final boolean ascending;

    public PageRequest(int page, int size) {
        this(page, size, "id", false);
    }

    public PageRequest(int page, int size, String sortField, boolean ascending) {
        if (page < 0) {
            throw new ValidationException("page must be >= 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new ValidationException("size must be between 1 and " + MAX_SIZE);
        }
        this.page = page;
        this.size = size;
        this.sortField = sortField;
        this.ascending = ascending;
    }

    public int getPage() { return page; }
    public int getSize() { return size; }
    public String getSortField() { return sortField; }
    public boolean isAscending() { return ascending; }

    public int getOffset() { return page * size; }
}
