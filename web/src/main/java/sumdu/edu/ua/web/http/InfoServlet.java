package sumdu.edu.ua.web.http;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sumdu.edu.ua.web.info.AppInfoService;

import java.io.IOException;

public class InfoServlet extends HttpServlet {
    private final AppInfoService info;

    public InfoServlet(AppInfoService info) {
        this.info = info;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSupport.writeJson(resp, HttpServletResponse.SC_OK, info.info());
    }
}
