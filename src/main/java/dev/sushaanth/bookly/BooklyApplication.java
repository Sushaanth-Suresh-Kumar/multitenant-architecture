package dev.sushaanth.bookly;

import dev.sushaanth.bookly.multitenancy.TenantHttpProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@EnableConfigurationProperties(TenantHttpProperties.class)
@SpringBootApplication
public class BooklyApplication {

	public static void main(String[] args) {
		SpringApplication.run(BooklyApplication.class, args);
	}

}
