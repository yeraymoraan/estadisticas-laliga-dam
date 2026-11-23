package es.fraydiegotadeo.laliga.importer;

import es.fraydiegotadeo.laliga.model.PartidoHistorico;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas unitarias del lector de CSV. Se usa un texto en memoria (StringReader) en lugar de
 * un archivo, así la prueba no depende de tener descargado ningún CSV.
 */
class CsvPartidosParserTest {

    @Test
    void importaResultadosYConservaNulos() throws Exception {
        // Fila 1: sin tarjetas. Fila 2: sin córners. Fila 3: partido sin jugar (sin goles).
        String csv = """
                Div,Date,HomeTeam,AwayTeam,FTHG,FTAG,HC,AC,HY,AY
                SP1,16/08/2025,Equipo A,Equipo B,2,1,6,4,,
                SP1,17/08/2025,Equipo C,Equipo D,0,0,,,2,1
                SP1,18/08/2025,Equipo E,Equipo F,,,2,3,1,1
                """;

        List<PartidoHistorico> partidos = new CsvPartidosParser().leer(new StringReader(csv));

        assertEquals(2, partidos.size()); // El tercero está pendiente y no se importa
        assertEquals(2, partidos.get(0).golesLocal());
        assertEquals(6, partidos.get(0).cornersLocal());
        assertNull(partidos.get(0).amarillasLocal()); // Dato ausente → null, no 0
        assertNull(partidos.get(1).cornersLocal());
    }

    @Test
    void aceptaColumnasSinNombre() throws Exception {
        // Los CSV antiguos (por ejemplo el de 1995/96) acaban la cabecera con columnas vacías: ",,,"
        String csv = """
                Div,Date,HomeTeam,AwayTeam,FTHG,FTAG,,,
                SP1,02/09/95,La Coruna,Valencia,3,0,,,
                """;

        List<PartidoHistorico> partidos = new CsvPartidosParser().leer(new StringReader(csv));

        assertEquals(1, partidos.size());
        assertEquals(1995, partidos.get(0).fecha().getYear());
        assertNull(partidos.get(0).cornersLocal()); // En 1995 no hay córners → null
    }

    @Test
    void rechazaCsvSinCabecerasObligatorias() {
        String csv = "Date,HomeTeam\n16/08/2025,Equipo A\n";

        assertThrows(IOException.class, () -> new CsvPartidosParser().leer(new StringReader(csv)));
    }
}
