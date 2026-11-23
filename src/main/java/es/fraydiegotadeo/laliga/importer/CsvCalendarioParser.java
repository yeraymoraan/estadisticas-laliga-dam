package es.fraydiegotadeo.laliga.importer;

import es.fraydiegotadeo.laliga.model.PartidoPendiente;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee el calendario de la próxima jornada (fixtures.csv de football-data.co.uk).
 *
 * Ese archivo trae los próximos partidos de muchas ligas europeas. Nos quedamos solo con los de
 * Primera División (Div = SP1). Usa los mismos nombres de equipo que los CSV de resultados,
 * así que se pueden cruzar directamente con los partidos guardados en MySQL.
 */
public final class CsvCalendarioParser {

    /** Dirección del calendario en Football-Data. */
    public static final String URL_CALENDARIO = "https://www.football-data.co.uk/fixtures.csv";

    /**
     * Descarga el calendario de Football-Data y lo guarda en un archivo.
     *
     * Usa HttpClient, que viene incluido en Java desde la versión 11. Hay que seguir las
     * redirecciones porque la web responde primero con un 302 (igual que pasaba con curl).
     *
     * @param destino archivo donde se guarda (por ejemplo data/fixtures.csv)
     * @throws IOException si no hay conexión o el servidor no responde con un 200 (OK)
     */
    public void descargar(Path destino) throws IOException {
        HttpClient cliente = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(URL_CALENDARIO))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        try {
            Files.createDirectories(destino.toAbsolutePath().getParent());
            HttpResponse<Path> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofFile(destino));
            if (respuesta.statusCode() != 200) {
                throw new IOException("Football-Data ha respondido con el código " + respuesta.statusCode());
            }
        } catch (InterruptedException error) {
            // Si se interrumpe el hilo hay que volver a marcarlo como interrumpido
            Thread.currentThread().interrupt();
            throw new IOException("Descarga interrumpida", error);
        }
    }

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
