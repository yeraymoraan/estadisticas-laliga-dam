package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.model.PartidoVista;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Pruebas del cálculo de aciertos y del Brier score. */
class BacktestingServiceTest {

    @Test
    void brierEsCeroSiSeAciertaConTodaSeguridad() {
        assertEquals(0.0, BacktestingService.brier(1, 0, 0, "1"), 0.0);
    }

    @Test
    void brierEsDosSiSeFallaConTodaSeguridad() {
        // Se dijo 100 % local y ganó el visitante: (1-0)² + 0² + (0-1)² = 2
        assertEquals(2.0, BacktestingService.brier(1, 0, 0, "2"), 0.0);
    }

    @Test
    void elPrimerPartidoNoSeEvaluaPorqueNoHayNadaAnterior() {
        LocalDate dia = LocalDate.of(2025, 8, 16);
        List<PartidoVista> partidos = List.of(
                partido(dia, "A", "B", 2, 0),
                partido(dia.plusDays(7), "B", "A", 0, 1));

        BacktestingService.Resultado resultado = new BacktestingService().evaluar(partidos, partidos);

        assertEquals(1, resultado.partidos());
        assertNull(new BacktestingService().evaluar(partidos, List.of(partidos.get(0))));
    }

    private static PartidoVista partido(LocalDate fecha, String local, String visitante,
                                        int golesLocal, int golesVisitante) {
        return new PartidoVista(fecha, local, visitante, golesLocal, golesVisitante,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
