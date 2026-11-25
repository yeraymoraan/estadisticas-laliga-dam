package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.model.PartidoVista;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JornadasServiceTest {

    private static final LocalDate DIA = LocalDate.of(2026, 8, 15);

    @Test
    void reparteLosPartidosEnJornadas() {
        // Liga de 4 equipos: dos partidos por jornada
        List<PartidoVista> partidos = List.of(
                partido(0, "A", "B"), partido(0, "C", "D"),     // jornada 1
                partido(7, "A", "C"), partido(8, "B", "D"),     // jornada 2
                partido(14, "D", "A"), partido(14, "B", "C"));  // jornada 3

        List<JornadasService.Jornada> jornadas = JornadasService.agrupar(partidos);

        assertEquals(3, jornadas.size());
        assertEquals(2, jornadas.get(0).partidos().size());
        assertEquals("A", jornadas.get(1).partidos().get(0).local());
        assertEquals(3, jornadas.get(2).numero());
    }

    @Test
    void unAplazadoQuedaEnLaJornadaEnQueSeJuega() {
        // C - D (jornada 1) se aplaza y se juega después de la jornada 2
        List<PartidoVista> partidos = List.of(
                partido(0, "A", "B"),
                partido(7, "A", "C"), partido(7, "B", "D"),
                partido(10, "C", "D"));

        List<JornadasService.Jornada> jornadas = JornadasService.agrupar(partidos);

        assertEquals(1, jornadas.get(0).partidos().size());
        assertEquals(3, jornadas.get(1).partidos().size()); // El aplazado cae en la jornada 2
    }

    private static PartidoVista partido(int dias, String local, String visitante) {
        return new PartidoVista(DIA.plusDays(dias), local, visitante, 1, 0,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
