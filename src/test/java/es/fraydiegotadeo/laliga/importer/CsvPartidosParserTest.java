package es.fraydiegotadeo.laliga.importer;

import org.junit.jupiter.api.Test;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;

class CsvPartidosParserTest {
    @Test void importaResultadosYConservaNulos() throws Exception {
        String csv = """
            Div,Date,HomeTeam,AwayTeam,FTHG,FTAG,HC,AC,HY,AY
            SP1,16/08/2025,Equipo A,Equipo B,2,1,6,4,,
            SP1,17/08/2025,Equipo C,Equipo D,0,0,,,2,1
            SP1,18/08/2025,Equipo E,Equipo F,,,2,3,1,1
            """;
        var registros = new CsvPartidosParser().leer(new StringReader(csv));
        assertEquals(2, registros.size()); // El tercero está pendiente y no se importa.
        assertEquals(2, registros.get(0).golesLocal());
        assertEquals(6, registros.get(0).cornersLocal());
        assertNull(registros.get(0).amarillasLocal());
        assertNull(registros.get(1).cornersLocal());
    }

    @Test void rechazaCSVSinCabecerasObligatorias() {
        String csv = "Date,HomeTeam\n16/08/2025,Equipo A\n";
        assertThrows(java.io.IOException.class,
                () -> new CsvPartidosParser().leer(new StringReader(csv)));
    }
}
