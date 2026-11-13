package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.model.PartidoVista;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Comprueba el cálculo de la clasificación con un ejemplo que se puede hacer a mano. */
class ClasificacionServiceTest {

    @Test
    void calculaPuntosYBalance() {
        // A gana 2-1 en casa (3 puntos) y empata 0-0 fuera (1 punto) → A: 4 puntos, DG +1; B: 1 punto
        LocalDate dia = LocalDate.of(2025, 8, 16);
        List<PartidoVista> partidos = List.of(
                partido(dia, "A", "B", 2, 1),
                partido(dia.plusDays(1), "B", "A", 0, 0));

        List<ClasificacionService.Fila> tabla = new ClasificacionService().calcular(partidos);

        assertEquals("A", tabla.get(0).equipo());
        assertEquals(4, tabla.get(0).puntos());
        assertEquals(1, tabla.get(0).diferencia());
        assertEquals(1, tabla.get(1).puntos());
    }

    /** Crea un partido de prueba solo con el resultado (el resto de estadísticas a null). */
    private static PartidoVista partido(LocalDate fecha, String local, String visitante,
                                        int golesLocal, int golesVisitante) {
        return new PartidoVista(fecha, local, visitante, golesLocal, golesVisitante,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
