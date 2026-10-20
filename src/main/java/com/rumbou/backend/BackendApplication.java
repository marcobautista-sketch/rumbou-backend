package com.rumbou.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class BackendApplication {

	// Los postulantes estan en Peru: rachas, simulacros y vencimientos se cuentan en
	// hora de Lima aunque el servidor (AWS) corra en UTC.
	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
		SpringApplication.run(BackendApplication.class, args);
	}

}
