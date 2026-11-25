package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.importer.ClienteFootballData;
import es.fraydiegotadeo.laliga.importer.CsvPartidosParser;
import es.fraydiegotadeo.laliga.model.PartidoHistorico;
import es.fraydiegotadeo.laliga.model.PartidoVista;
import es.fraydiegotadeo.laliga.repository.PartidosRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Mantiene la base de datos al día sin que el usuario tenga que importar nada.
 *
 * - La primera vez (o si falta alguna temporada) descarga todo el histórico desde 1993/94.
 * - Siempre vuelve a descargar la temporada en curso, para añadir los partidos nuevos.
 *   Como el repositorio no duplica partidos, reimportar solo añade o actualiza.
 *
 * Si no hay internet no se pierde nada: la aplicación sigue con lo que ya estaba guardado.
 */
public final class ActualizacionService {

    /** Primera temporada que tiene Football-Data en CSV. */
    public static final int PRIMERA_TEMPORADA = 1993;

    /** En julio ya se considera que empieza la temporada nueva (la liga arranca en agosto). */
    private static final int MES_INICIO_TEMPORADA = 7;

    /**
     * Resumen de la actualización para mostrarlo en la ventana.
     *
     * @param temporadasActualizadas temporadas descargadas e importadas
     * @param ultimoPartido          fecha del último partido guardado de la temporada en curso (null si no hay)
     * @param avisos                 problemas que no impiden seguir (sin conexión, temporada sin empezar...)
     */
    public record Resumen(int temporadasActualizadas, LocalDate ultimoPartido, List<String> avisos) {
    }

    private final PartidosRepository repositorio;
    private final ClienteFootballData descargas;
    private final CsvPartidosParser parser = new CsvPartidosParser();

    public ActualizacionService(PartidosRepository repositorio, ClienteFootballData descargas) {
        this.repositorio = repositorio;
        this.descargas = descargas;
    }

    /**
     * Temporada que se está jugando en una fecha: de julio a diciembre es la que empieza ese año
     * y de enero a junio la que empezó el año anterior.
     *
     * @param hoy fecha de referencia
     * @return año de inicio (2026 = temporada 2026/27)
     */
    public static int temporadaEnCurso(LocalDate hoy) {
        if (hoy.getMonthValue() >= MES_INICIO_TEMPORADA) {
            return hoy.getYear();
        }
        return hoy.getYear() - 1;
    }

    /**
     * Temporadas que hay que descargar: las que faltan en la base de datos y siempre la que está en curso.
     *
     * @param guardadas temporadas que ya están en MySQL
     * @param enCurso   temporada que se está jugando
     * @return años de inicio, de la más antigua a la más reciente
     */
    public static List<Integer> temporadasPorDescargar(List<Integer> guardadas, int enCurso) {
        List<Integer> faltan = new ArrayList<>();
        for (int inicio = PRIMERA_TEMPORADA; inicio < enCurso; inicio++) {
            if (!guardadas.contains(inicio)) {
                faltan.add(inicio);
            }
        }
        faltan.add(enCurso);
        return faltan;
    }

    /**
     * Descarga e importa lo necesario.
     *
     * @param hoy fecha de hoy (se pasa como parámetro para poder probarlo con otras fechas)
     * @return resumen de lo que se ha hecho
     * @throws SQLException si falla MySQL (sin base de datos la aplicación no puede funcionar)
     */
    public Resumen actualizar(LocalDate hoy) throws SQLException {
        int enCurso = temporadaEnCurso(hoy);
        List<String> avisos = new ArrayList<>();
        int actualizadas = 0;

        for (int inicio : temporadasPorDescargar(repositorio.listarTemporadas(), enCurso)) {
            Path archivo = ClienteFootballData.archivoTemporada(inicio);
            try {
                descargas.descargar(ClienteFootballData.urlTemporada(inicio), archivo);
            } catch (IOException error) {
                // Sin conexión no tiene sentido seguir probando el resto de temporadas
                avisos.add("Sin conexión con Football-Data: se usan los datos guardados (" + error.getMessage() + ").");
                break;
            }
            try {
                List<PartidoHistorico> partidos = parser.leer(archivo);
                repositorio.importar(inicio, partidos);
                actualizadas++;
            } catch (IOException error) {
                // Por ejemplo, en julio la temporada nueva todavía no tiene partidos
                avisos.add("Temporada " + inicio + ": " + error.getMessage());
            }
        }
        return new Resumen(actualizadas, ultimoPartido(enCurso), avisos);
    }

    /** Fecha del último partido guardado de una temporada, o null si no tiene ninguno. */
    private LocalDate ultimoPartido(int inicio) throws SQLException {
        LocalDate ultimo = null;
        for (PartidoVista partido : repositorio.listarPartidos(inicio)) {
            if (ultimo == null || partido.fecha().isAfter(ultimo)) {
                ultimo = partido.fecha();
            }
        }
        return ultimo;
    }
}
