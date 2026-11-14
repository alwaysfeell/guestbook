package sumdu.edu.ua.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import sumdu.edu.ua.web.info.AppInfoService;

import java.util.Map;

@Controller
public class InfoController {
    private final AppInfoService info;

    public InfoController(AppInfoService info) {
        this.info = info;
    }

    @GetMapping("/info")
    @ResponseBody
    public Map<String, Object> info() {
        return info.info();
    }
}
