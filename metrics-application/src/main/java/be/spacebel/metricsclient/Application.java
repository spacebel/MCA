package be.spacebel.metricsclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * The class implements main() method that uses Spring Boot’s
 * SpringApplication.run() method to launch the application
 *
 * @author mng
 */
@ServletComponentScan
@SpringBootApplication(scanBasePackages = "be.spacebel")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
