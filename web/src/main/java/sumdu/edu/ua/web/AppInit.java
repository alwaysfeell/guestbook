package sumdu.edu.ua.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "sumdu.edu.ua")
@ConfigurationPropertiesScan("sumdu.edu.ua")
public class AppInit {

    public static void main(String[] args) {
        SpringApplication.run(AppInit.class, args);
    }
}
