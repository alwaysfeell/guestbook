package sumdu.edu.ua.persistence.jdbc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class Db {
    public static final String DEFAULT_URL = "jdbc:h2:file:./data/books;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASS = "";

    private final String url;

    public Db(@Value("${app.db.url:" + DEFAULT_URL + "}") String url) {
        this.url = url;
    }

    public Connection get() throws SQLException {
        return DriverManager.getConnection(url, USER, PASS);
    }

    public String getUrl() {
        return url;
    }
}
