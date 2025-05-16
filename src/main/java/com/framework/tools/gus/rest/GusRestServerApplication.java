package com.framework.tools.gus.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.framework.tools.gus")
public class GusRestServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GusRestServerApplication.class, args);
	}

}
