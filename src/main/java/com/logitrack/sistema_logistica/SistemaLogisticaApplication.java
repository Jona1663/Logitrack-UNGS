package com.logitrack.sistema_logistica;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SistemaLogisticaApplication {


	public static void main(String[] args) {
		System.setProperty("user.timezone", "UTC");  //para solucionar un error con docker y la zona horaria
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(SistemaLogisticaApplication.class, args);
	}

}
