package com.logitrack.sistema_logistica.controller;

import java.util.List; //Necesaria porque el método devuelve una lista (List<Envio>).
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity; //Es el objeto que Spring usa para envolver la respuesta (incluyendo el estado HTTP como 200 OK).
import org.springframework.security.core.Authentication; //Es el objeto que Spring Security inyecta automáticamente en el método para representar la identidad del usuario autenticado.
import org.springframework.web.bind.annotation.GetMapping;//Anotación que indica que este método responderá a solicitudes HTTP GET en la ruta "/envios" relativa al controlador.
import org.springframework.web.bind.annotation.RequestMapping;//Anotación que define la ruta base para todos los endpoints de este controlador. En este caso, "/api/chofer" significa que todos los métodos dentro de este controlador responderán a rutas que comiencen con "/api/chofer".
import org.springframework.web.bind.annotation.RestController;//Anotación que marca esta clase como un controlador REST, lo que significa que sus métodos devolverán datos (generalmente en formato JSON) en lugar de vistas HTML.

import com.logitrack.sistema_logistica.dto.EnvioResumenDTO;
import com.logitrack.sistema_logistica.model.Envio;//Importa la clase Envio, que es el modelo de datos que representa un envío en el sistema. Es necesario para definir el tipo de datos que se devolverá en la respuesta.
import com.logitrack.sistema_logistica.service.EnvioService;//Importa la clase EnvioService, que es el servicio que contiene la lógica de negocio para manejar los envíos. Se inyectará en el controlador para poder llamar a sus métodos.

import lombok.RequiredArgsConstructor;//Anotación de Lombok que genera un constructor con todos los campos finales (final) de la clase. En este caso, se usará para inyectar el EnvioService sin necesidad de escribir un constructor manualmente.

@RestController//Indica que esta clase es un controlador REST, lo que significa que sus métodos devolverán datos (generalmente en formato JSON) en lugar de vistas HTML.
@RequestMapping("/api/chofer")//Define la ruta base para todos los endpoints de este controlador. En este caso, "/api/chofer" significa que todos los métodos dentro de este controlador responderán a rutas que comiencen con "/api/chofer".
@RequiredArgsConstructor//Genera un constructor con todos los campos finales (final) de la clase. En este caso, se usará para inyectar el EnvioService sin necesidad de escribir un constructor manualmente.
public class ChoferController {//Define la clase ChoferController, que es el controlador REST que manejará las solicitudes relacionadas con los choferes.

    private final EnvioService envioService;
    /**
     * Endpoint #113: Retorna la lista de envíos asignados exclusivamente 
     * al chofer autenticado a través del token JWT.
     */

    @GetMapping("/envios")
    public ResponseEntity<List<EnvioResumenDTO>> getMisEnvios(Authentication authentication) {//Define un método que responderá a solicitudes HTTP GET en la ruta "/envios" relativa al controlador (es decir, "/api/chofer/envios"). El método devuelve un ResponseEntity que envuelve una lista de objetos Envio. El Authentication es inyectado automáticamente por Spring Security y representa la identidad del usuario autenticado. Se usará para obtener el username del chofer y luego buscar los envíos asociados a ese chofer.                              
        // El Authentication trae el "username" del JWT automáticamente
        String username = authentication.getName(); 

        // Obtenemos las entidades del servicio
        List<Envio> misEnvios = envioService.obtenerEnviosPorChofer(username);
        
        // Las transformamos a DTOs (esto oculta automáticamente la prioridad_ia)
        List<EnvioResumenDTO> resumen = misEnvios.stream()
                .map(EnvioResumenDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(resumen);
    }  
}