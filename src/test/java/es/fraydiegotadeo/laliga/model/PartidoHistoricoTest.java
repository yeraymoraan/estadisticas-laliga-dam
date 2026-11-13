package es.fraydiegotadeo.laliga.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** Pruebas de las validaciones de un partido (casos previstos en la primera entrega). */
class PartidoHistoricoTest {

    private static final LocalDate FECHA = LocalDate.of(2025, 8, 16);

    @Test
    void rechazaGolesNegativos() {
        assertThrows(IllegalArgumentException.class,
                () -> partido("Equipo A", "Equipo B", -1, 0));
    }

    @Test
    void rechazaMismoEquipoLocalYVisitante() {
        // Da igual que cambien las mayúsculas: sigue siendo el mismo equipo
        assertThrows(IllegalArgumentException.class,
                () -> partido("Equipo A", "equipo a", 1, 0));
    }

    private static PartidoHistorico partido(String local, String visitante, int golesLocal, int golesVisitante) {
        return new PartidoHistorico(FECHA, local, visitante, golesLocal, golesVisitante,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
