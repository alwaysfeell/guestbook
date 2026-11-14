package sumdu.edu.ua.web.info;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.web.AppProperties;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class AppInfoService {

    private final AppProperties props;
    private final BookService books;
    private final CommentService comments;

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private ApplicationContext context;

    public AppInfoService(AppProperties props, BookService books, CommentService comments) {
        this.props = props;
        this.books = books;
        this.comments = comments;
    }

    public Map<String, Object> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("application", applicationName);
        info.put("title", props.title());
        info.put("springBootVersion", SpringBootVersion.getVersion());
        info.put("serverPort", context.getEnvironment().getProperty("local.server.port",
                context.getEnvironment().getProperty("server.port", "8080")));
        info.put("defaultPageSize", props.catalog().defaultPageSize());
        info.put("commentDeleteWindow", comments.getDeleteWindow().toString());
        info.put("booksTotal", books.search(null, 0, 1, null).getTotal());
        info.put("beansInContext", context.getBeanDefinitionCount());
        info.put("applicationBeans", applicationBeans());
        info.put("endpoints", endpoints());
        return info;
    }

    public List<String> endpoints() {
        RequestMappingHandlerMapping mapping =
                context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        List<String> result = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> e : mapping.getHandlerMethods().entrySet()) {
            HandlerMethod handler = e.getValue();
            if (!handler.getBeanType().getName().startsWith("sumdu.edu.ua")) {
                continue;
            }
            String methods = e.getKey().getMethodsCondition().getMethods().stream()
                    .map(Enum::name).sorted().collect(Collectors.joining(","));
            if (methods.isEmpty()) {
                methods = "ANY";
            }
            for (String path : e.getKey().getPatternValues()) {
                result.add(String.format("%-6s %-16s -> %s.%s()", methods, path,
                        handler.getBeanType().getSimpleName(), handler.getMethod().getName()));
            }
        }
        result.sort(String::compareTo);
        return result;
    }

    public Map<String, String> applicationBeans() {
        Map<String, String> beans = new TreeMap<>();
        for (String name : context.getBeanDefinitionNames()) {
            Class<?> type = context.getType(name);
            if (type == null) {
                continue;
            }
            type = ClassUtils.getUserClass(type);
            if (type.getName().startsWith("sumdu.edu.ua")
                    || Clock.class.isAssignableFrom(type)
                    || DispatcherServlet.class.isAssignableFrom(type)) {
                beans.put(name, type.getSimpleName());
            }
        }
        return beans;
    }
}
