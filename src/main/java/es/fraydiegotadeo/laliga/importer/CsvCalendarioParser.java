package es.fraydiegotadeo.laliga.importer;

import es.fraydiegotadeo.laliga.model.PartidoPendiente;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee el calendario de la próxima jornada (fixtures.csv de football-data.co.uk, que descarga
 * ClienteFootballData).
 *
 * Ese archivo trae los próximos partidos de muchas ligas europeas. Nos quedamos solo con los de
 * Primera División (Div = SP1). Usa los mismos nombres de equipo que los CSV de resultados,
 * así que se pueden cruzar directamente con los partidos guardados en MySQL.
 */
public final class CsvCalendarioParser {

    /**
     * Lee el calendario desde un archivo del disco.
     *
     * @param archivo ruta de fixtures.csv
     * @return partidos de LaLiga pendientes de jugar
     * @throws IOException si no se puede leer o falta alguna columna
     */
    public List<PartidoPendiente> leer(Path archivo) throws IOException {
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            return leer(lector);
        }
    }

    /** Igual que leer(Path), pero desde un Reader para poder probarlo con un texto en memoria. */
    public List<PartidoPendiente> leer(Reader lector) throws IOException {
        BufferedReader buffer = new BufferedReader(lector);
        CsvPartidosParser.saltarBom(buffer);

        List<PartidoPendiente> partidos = new ArrayList<>();
        try (CSVParser parser = CsvPartidosParser.FORMATO.parse(buffer)) {
            List<String> cabeceras = parser.getHeaderNames();
            if (!cabeceras.contains("Div") || !cabeceras.contains("Date")
                    || !cabeceras.contains("HomeTeam") || !cabeceras.contains("AwayTeam")) {
                throw new IOException("El calendario no tiene las columnas Div, Date, HomeTeam y AwayTeam.");
            }

            for (CSVRecord fila : parser) {
                // Solo Primera División española
                if (!CsvPartidosParser.texto(fila, "Div").equals("SP1")) {
                    continue;
                }
                try {
                    partidos.add(new PartidoPendiente(
                            CsvPartidosParser.leerFecha(CsvPartidosParser.texto(fila, "Date")),
                            CsvPartidosParser.texto(fila, "HomeTeam"),
                            CsvPartidosParser.texto(fila, "AwayTeam")));
                } catch (DateTimeException error) {
                    throw new IOException("Fecha incorrecta en la línea " + fila.getRecordNumber(), error);
                }
            }
        }
        return partidos;
    }
}
