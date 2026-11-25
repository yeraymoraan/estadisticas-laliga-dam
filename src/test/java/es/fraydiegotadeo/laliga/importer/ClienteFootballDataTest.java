package es.fraydiegotadeo.laliga.importer;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Comprueba que se construyen bien las direcciones de Football-Data (sin conectarse a internet). */
class ClienteFootballDataTest {

    @Test
    void codigoDeTemporadaConDosCifrasPorAnio() {
        assertEquals("2627", ClienteFootballData.codigoTemporada(2026));
        assertEquals("9394", ClienteFootballData.codigoTemporada(1993));
        // El cambio de siglo: 1999/2000 es "9900"
        assertEquals("9900", ClienteFootballData.codigoTemporada(1999));
        assertEquals("0001", ClienteFootballData.codigoTemporada(2000));
    }

    @Test
    void direccionYArchivoDeUnaTemporada() {
        assertEquals("https://www.football-data.co.uk/mmz4281/2526/SP1.csv",
                ClienteFootballData.urlTemporada(2025));
        assertEquals(Path.of("data", "SP1_2526.csv"), ClienteFootballData.archivoTemporada(2025));
    }
}
