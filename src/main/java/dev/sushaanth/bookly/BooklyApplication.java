package dev.sushaanth.bookly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class BooklyApplication {

	public static void main(String[] args) {
		SpringApplication.run(BooklyApplication.class, args);
	}

}
