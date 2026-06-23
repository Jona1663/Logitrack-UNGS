package com.logitrack.sistema_logistica.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.logitrack.sistema_logistica.dto.CartaPorteDTO;

@ExtendWith(MockitoExtension.class)
public class CartaPortePdfServiceTest {

    @Mock
    private CartaPorteService cartaPorteService; // Este es el que inyectás en el servicio

    @InjectMocks
    private CartaPortePdfService cartaPortePdfService; // Tu servicio de PDF

    @Test
    public void generarPdf_DeberiaRetornarByteArray() {
        // Arrange
        String idEnvio = "LT-123";
        CartaPorteDTO dtoMock = new CartaPorteDTO();
        dtoMock.setIdEnvio(idEnvio);
        
        // Mockeamos la llamada interna al otro servicio
        when(cartaPorteService.obtenerCartaPorte(idEnvio)).thenReturn(dtoMock);

        // Act
        byte[] resultado = cartaPortePdfService.generarPdf(idEnvio);

        // Assert
        assertNotNull(resultado, "El PDF no debería ser nulo");
    }
}