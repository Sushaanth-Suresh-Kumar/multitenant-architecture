package dev.sushaanth;

import dev.sushaanth.bookly.multitenancy.TenantHttpProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@EnableConfigurationProperties(TenantHttpProperties.class)
@SpringBootApplication
public class LibraryManagementBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibraryManagementBackendApplication.class, args);
	}

}
