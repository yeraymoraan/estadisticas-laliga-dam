package es.fraydiegotadeo.laliga.importer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Descarga los archivos CSV de football-data.co.uk, para que el usuario no tenga que importarlos a mano.
 *
 * Football-Data guarda cada temporada de Primera División en una dirección fija:
 * https://www.football-data.co.uk/mmz4281/2627/SP1.csv es la temporada 2026/27
 * (2627 = las dos últimas cifras del año de inicio y del año de fin).
 */
public final class ClienteFootballData {

    private static final String URL_BASE = "https://www.football-data.co.uk/mmz4281/";

    /** Dirección del calendario de la próxima jornada (todas las ligas). */
    public static final String URL_CALENDARIO = "https://www.football-data.co.uk/fixtures.csv";

    /**
     * Un solo HttpClient para todas las descargas. Sigue las redirecciones porque la web
     * responde primero con un 302 (igual que pasaba con curl, que necesitaba la opción -L).
     */
    private final HttpClient cliente = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Código de la temporada en las direcciones de Football-Data.
     *
     * @param inicio año de inicio de la temporada (2026 = 2026/27)
     * @return por ejemplo "2627" para 2026 o "9900" para 1999
     */
    public static String codigoTemporada(int inicio) {
        return String.format("%02d%02d", inicio % 100, (inicio + 1) % 100);
    }

    /** @return dirección del CSV de Primera División de esa temporada */
    public static String urlTemporada(int inicio) {
        return URL_BASE + codigoTemporada(inicio) + "/SP1.csv";
    }

    /** @return archivo donde se guarda el CSV de esa temporada, por ejemplo data/SP1_2627.csv */
    public static Path archivoTemporada(int inicio) {
        return Path.of("data", "SP1_" + codigoTemporada(inicio) + ".csv");
    }

    /**
     * Descarga una dirección y la guarda en un archivo (lo sobrescribe si ya existe).
     *
     * @param url     dirección del archivo
     * @param destino dónde se guarda
     * @throws IOException si no hay conexión o el servidor no responde con un 200 (OK)
     */
    public void descargar(String url, Path destino) throws IOException {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
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
}
