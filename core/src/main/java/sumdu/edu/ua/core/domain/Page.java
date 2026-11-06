package sumdu.edu.ua.core.domain;

import java.util.List;

public class Page<T> {
    private final List<T> items;
    private final int page;
    private final int size;
    private final long total;

    public Page(List<T> items, PageRequest request, long total) {
        this.items = List.copyOf(items);
        this.page = request.getPage();
        this.size = request.getSize();
        this.total = total;
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotal() { return total; }

    public long getTotalPages() {
        return (total + size - 1) / size;
    }
}
