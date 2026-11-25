package es.fraydiegotadeo.laliga.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pruebas de qué temporadas se descargan (sin conectarse a internet ni a MySQL). */
class ActualizacionServiceTest {

    @Test
    void temporadaEnCursoSegunElMes() {
        // Noviembre de 2026 → 2026/27; marzo de 2027 → sigue siendo la 2026/27
        assertEquals(2026, ActualizacionService.temporadaEnCurso(LocalDate.of(2026, 11, 25)));
        assertEquals(2026, ActualizacionService.temporadaEnCurso(LocalDate.of(2027, 3, 1)));
        assertEquals(2026, ActualizacionService.temporadaEnCurso(LocalDate.of(2026, 7, 1)));
        assertEquals(2025, ActualizacionService.temporadaEnCurso(LocalDate.of(2026, 6, 30)));
    }

    @Test
    void conLaBaseVaciaSeDescargaTodoElHistorico() {
        List<Integer> porDescargar = ActualizacionService.temporadasPorDescargar(List.of(), 2026);

        assertEquals(34, porDescargar.size()); // De 1993/94 a 2026/27
        assertEquals(1993, porDescargar.get(0));
        assertEquals(2026, porDescargar.get(33));
    }

    @Test
    void conTodoGuardadoSoloSeDescargaLaTemporadaEnCurso() {
        List<Integer> guardadas = ActualizacionService.temporadasPorDescargar(List.of(), 2026);

        assertEquals(List.of(2026), ActualizacionService.temporadasPorDescargar(guardadas, 2026));
    }

    @Test
    void siFaltaUnaTemporadaAntiguaSeDescargaTambien() {
        List<Integer> guardadas = List.of(2026, 2025, 2024);

        List<Integer> porDescargar = ActualizacionService.temporadasPorDescargar(guardadas, 2026);

        // Faltan las 31 temporadas de 1993 a 2023, y la 2026 (en curso) se descarga siempre: 32
        assertEquals(32, porDescargar.size());
        assertEquals(2023, porDescargar.get(30));
        assertEquals(2026, porDescargar.get(31));
    }
}
