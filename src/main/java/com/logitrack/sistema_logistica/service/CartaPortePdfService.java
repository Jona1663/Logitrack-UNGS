package com.logitrack.sistema_logistica.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.logitrack.sistema_logistica.dto.CartaPorteDTO;
import lombok.RequiredArgsConstructor;

@Service
public class CartaPortePdfService {
    // Inyectamos el servicio que ya tiene los datos limpios y listos para mostrar
    @Autowired
    private CartaPorteService cartaPorteService;

    public byte[] generarPdf(String idEnvio) {
        // Obtenemos los datos limpios
        CartaPorteDTO dto = cartaPorteService.obtenerCartaPorte(idEnvio);

        // Preparamos el documento (Hoja A4) y el flujo de salida en memoria
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fuentes (Tipografías)
            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Font subTituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
            Font textoFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);

            // Encabezado del PDF (Logo y Título)
            Paragraph membrete = new Paragraph("LOGITRACK LOGÍSTICA S.A.", tituloFont);
            membrete.setAlignment(Element.ALIGN_CENTER);
            document.add(membrete);

            document.add(new Paragraph(" ")); // Espacio en blanco

            Paragraph titulo = new Paragraph("CARTA DE PORTE ELECTRÓNICA (CPE)", tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            // Fecha y Hora de emisión
            String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            Paragraph fecha = new Paragraph("Fecha de emisión: " + fechaActual, textoFont);
            fecha.setAlignment(Element.ALIGN_RIGHT);
            document.add(fecha);

            document.add(new Paragraph(" "));

            // Tabla con los datos legales (2 columnas)
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);

            // Agregamos celdas a la tabla
            agregarFila(table, "Nro. Envío:", dto.getIdEnvio(), subTituloFont, textoFont);
            agregarFila(table, "Nro. CPE:", dto.getCpe(), subTituloFont, textoFont);
            agregarFila(table, "Autorización ARCA (CTG):", dto.getAutorizacionArca(), subTituloFont, textoFont);
            agregarFila(table, "Patente del Camión:", dto.getPatenteCamion(), subTituloFont, textoFont);
            agregarFila(table, "Chofer:", dto.getNombreChofer(), subTituloFont, textoFont);
            agregarFila(table, "CUIL Chofer:", dto.getCuilChofer(), subTituloFont, textoFont);
            agregarFila(table, "Licencia Chofer:", dto.getLicenciaChofer(), subTituloFont, textoFont);
            agregarFila(table, "Tipo de Grano:", dto.getTipoGrano(), subTituloFont, textoFont);
            agregarFila(table, "Peso Estimado (Kg):", String.valueOf(dto.getPesoEstimadoKg()), subTituloFont,
                    textoFont);
            agregarFila(table, "Origen:", dto.getOrigen(), subTituloFont, textoFont);
            agregarFila(table, "Destino:", dto.getDestino(), subTituloFont, textoFont);

            document.add(table);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // pie de página
            Paragraph piePagina = new Paragraph(
                    "Documento generado electrónicamente. Válido como comprobante en tránsito.", subTituloFont);
            piePagina.setAlignment(Element.ALIGN_CENTER);
            document.add(piePagina);

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de la Carta de Porte", e);
        } finally {
            document.close();
        }

        // Convertimos el documento a un array de bytes para poder mandarlo por HTTP
        return out.toByteArray();
    }

    // Método auxiliar para dibujar las filas de la tabla de forma limpia
    private void agregarFila(PdfPTable table, String etiqueta, String valor, Font fontEtiqueta, Font fontValor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, fontEtiqueta));
        celdaEtiqueta.setBorderColor(Color.LIGHT_GRAY);
        celdaEtiqueta.setPadding(8f);
        celdaEtiqueta.setBackgroundColor(new Color(240, 240, 240)); // Gris muy clarito

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor != null ? valor : "N/A", fontValor));
        celdaValor.setBorderColor(Color.LIGHT_GRAY);
        celdaValor.setPadding(8f);

        table.addCell(celdaEtiqueta);
        table.addCell(celdaValor);
    }
}
