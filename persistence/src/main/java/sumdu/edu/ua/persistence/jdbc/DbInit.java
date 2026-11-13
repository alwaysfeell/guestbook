package sumdu.edu.ua.persistence.jdbc;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class DbInit {
    private static final Logger log = LoggerFactory.getLogger(DbInit.class);

    private final Db db;

    public DbInit(Db db) {
        this.db = db;
    }

    @PostConstruct
    public void init() {
        try (Connection c = db.get(); Statement st = c.createStatement()) {
            runScript(st, "schema.sql");
            try (ResultSet rs = st.executeQuery("select count(*) from books")) {
                rs.next();
                if (rs.getLong(1) == 0) {
                    runScript(st, "data.sql");
                    log.info("Demo data inserted (data.sql)");
                }
            }
            log.info("Database schema is ready (tables books, comments), url={}", db.getUrl());
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("DB schema init failed", e);
        }
    }

    private static void runScript(Statement st, String resource) throws IOException, SQLException {
        try (InputStream in = DbInit.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(resource + " not found in resources");
            }
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String cmd : sql.split(";")) {
                if (!cmd.isBlank()) {
                    st.execute(cmd);
                }
            }
        }
    }
}
