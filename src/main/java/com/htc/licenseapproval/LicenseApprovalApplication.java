package com.htc.licenseapproval;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "auditorAware") 
public class LicenseApprovalApplication {

	public static void main(String[] args) {
		SpringApplication.run(LicenseApprovalApplication.class, args);
	}

	@Bean
    ModelMapper modelMapper() {
		return new ModelMapper();
	}
}