package sumdu.edu.ua.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.web.error.ErrorFilter;
import sumdu.edu.ua.web.error.ErrorPageServlet;
import sumdu.edu.ua.web.http.BooksServlet;
import sumdu.edu.ua.web.http.CommentsServlet;
import sumdu.edu.ua.web.http.InfoServlet;
import sumdu.edu.ua.web.info.AppInfoService;

@Configuration
public class ServletConfig {

    @Bean
    public ServletRegistrationBean<BooksServlet> booksServlet(BookService books,
        CommentService comments,
        AppProperties props) {
        BooksServlet servlet = new BooksServlet(books, comments, props.catalog().defaultPageSize());
        return new ServletRegistrationBean<>(servlet, "/books/*");
    }

    @Bean
    public ServletRegistrationBean<CommentsServlet> commentsServlet(CommentService comments,
        AppProperties props) {
        CommentsServlet servlet = new CommentsServlet(comments, props.catalog().defaultPageSize());
        return new ServletRegistrationBean<>(servlet, "/comments/*");
    }

    @Bean
    public ServletRegistrationBean<InfoServlet> infoServlet(AppInfoService info) {
        return new ServletRegistrationBean<>(new InfoServlet(info), "/info");
    }

    @Bean
    public ServletRegistrationBean<ErrorPageServlet> errorPageServlet() {
        return new ServletRegistrationBean<>(new ErrorPageServlet(), "/error");
    }

    @Bean
    public FilterRegistrationBean<ErrorFilter> errorFilter() {
        FilterRegistrationBean<ErrorFilter> reg = new FilterRegistrationBean<>(new ErrorFilter());
        reg.addUrlPatterns("/*");
        return reg;
    }
}
