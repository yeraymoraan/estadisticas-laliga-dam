package es.fraydiegotadeo.laliga.importer;

import es.fraydiegotadeo.laliga.model.PartidoPendiente;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pruebas del lector del calendario (fixtures.csv). */
class CsvCalendarioParserTest {

    @Test
    void soloLeeLosPartidosDePrimeraDivision() throws Exception {
        // E0 es la Premier League inglesa: se tiene que ignorar
        String csv = """
                Div,Date,Time,HomeTeam,AwayTeam,AvgH
                E0,21/11/2026,13:30,Arsenal,Chelsea,2.1
                SP1,22/11/2026,20:00,Barcelona,Villarreal,1.4
                """;

        List<PartidoPendiente> partidos = new CsvCalendarioParser().leer(new StringReader(csv));

        assertEquals(1, partidos.size());
        assertEquals("Barcelona", partidos.get(0).local());
        assertEquals(LocalDate.of(2026, 11, 22), partidos.get(0).fecha());
    }
}
