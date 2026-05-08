package com.logitrack.sistema_logistica;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class IntegracionCapasTest {

    // Inyectamos el cerebro de Spring Boot (ApplicationContext)
    // Esto contiene TODAS las capas reales de tu aplicación conectadas entre sí
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void validarComunicacionDeCapas_elContextoDebeCargarSinErrores() {
        // GIVEN: El framework Spring Boot intenta levantar la aplicación completa
        
        // WHEN: Revisamos las dependencias internas del sistema
        Object controller = applicationContext.getBean("envioController");
        Object service = applicationContext.getBean("envioService");
        Object repository = applicationContext.getBean("envioRepository");

        // THEN: Validamos que las 3 capas se comunican correctamente. 
        // Si el Service no pudiera hablar con el Repository, Spring no podría crear el Controller y esto fallaría.
        assertNotNull(controller, "La capa de Controladores (API) debe estar cargada y conectada");
        assertNotNull(service, "La capa de Lógica de Negocio (Service) debe estar conectada al Controller");
        assertNotNull(repository, "La capa de Acceso a Datos (Repository) debe estar conectada al Service");
        
        System.out.println("✅ ÉXITO: Las capas Controller -> Service -> Repository se comunican perfectamente.");
    }
}