package gg.nya.imagehosting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class ImagehostingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImagehostingApplication.class, args);
	}
}
