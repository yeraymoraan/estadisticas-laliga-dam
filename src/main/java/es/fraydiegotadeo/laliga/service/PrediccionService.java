package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.importer.ClienteFootballData;
import es.fraydiegotadeo.laliga.importer.CsvCalendarioParser;
import es.fraydiegotadeo.laliga.model.PartidoPendiente;
import es.fraydiegotadeo.laliga.model.PartidoVista;
import es.fraydiegotadeo.laliga.repository.PartidosRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prepara la predicción de la próxima jornada: descarga el calendario, carga de MySQL los partidos
 * de la temporada actual y la anterior y predice cada partido con el modelo de Poisson.
 *
 * Antes este código estaba en App.java; se ha pasado a la capa service para que la interfaz
 * solo se encargue de mostrar los datos.
 */
public final class PrediccionService {

    /** Archivo donde se guarda el último calendario descargado. */
    private static final Path ARCHIVO_CALENDARIO = Path.of("data", "fixtures.csv");

    /**
     * Un partido del calendario con su predicción y, si ya se ha jugado, su resultado real.
     *
     * Football-Data tarda unos días en cambiar el calendario a la jornada siguiente, así que a veces
     * el calendario trae partidos que ya tienen resultado. La predicción se sigue haciendo solo con
     * los partidos anteriores a su fecha, así que es justo compararla con lo que pasó.
     *
     * @param resultadoReal el partido ya jugado, o null si todavía no se ha jugado
     */
    public record PartidoPredicho(Prediccion prediccion, PartidoVista resultadoReal) {

        public boolean jugado() {
            return resultadoReal != null;
        }

        /** @return true si el 1-X-2 más probable fue el que pasó (solo tiene sentido si está jugado) */
        public boolean acertado() {
            return jugado() && prediccion.signoMasProbable().equals(BacktestingService.signoReal(resultadoReal));
        }
    }

    /**
     * Resultado de predecir la jornada.
     *
     * @param partidos      un elemento por partido del calendario
     * @param sinConexion   true si no se pudo descargar y se usó el último calendario guardado
     * @param fiabilidad    backtesting de la temporada anterior completa (null si no hay datos)
     * @param temporadaEvaluada año de inicio de la temporada usada en el backtesting
     */
    public record Jornada(List<PartidoPredicho> partidos, boolean sinConexion,
                          BacktestingService.Resultado fiabilidad, int temporadaEvaluada) {
    }

    private final PartidosRepository repositorio;
    private final ClienteFootballData descargas;
    private final CsvCalendarioParser calendario = new CsvCalendarioParser();

    public PrediccionService(PartidosRepository repositorio, ClienteFootballData descargas) {
        this.repositorio = repositorio;
        this.descargas = descargas;
    }

    /**
     * @return las predicciones de la próxima jornada y la fiabilidad del modelo
     * @throws IOException  si no hay calendario (ni descargado ni guardado)
     * @throws SQLException si falla MySQL
     * @throws IllegalStateException si todavía no hay partidos guardados
     */
    public Jornada predecirProximaJornada() throws IOException, SQLException {
        boolean sinConexion = false;
        try {
            descargas.descargar(ClienteFootballData.URL_CALENDARIO, ARCHIVO_CALENDARIO);
        } catch (IOException error) {
            // Sin internet se usa el último calendario descargado, si existe
            if (!Files.exists(ARCHIVO_CALENDARIO)) {
                throw error;
            }
            sinConexion = true;
        }
        List<PartidoPendiente> pendientes = calendario.leer(ARCHIVO_CALENDARIO);

        List<Integer> temporadas = repositorio.listarTemporadas();
        if (temporadas.isEmpty()) {
            throw new IllegalStateException("Todavía no hay partidos guardados. Espera a que termine la descarga.");
        }
        // listarTemporadas devuelve primero la más reciente
        int actual = temporadas.get(0);
        List<PartidoVista> anterior = repositorio.listarPartidos(actual - 1);
        List<PartidoVista> temporadaActual = repositorio.listarPartidos(actual);
        List<PartidoVista> historial = new ArrayList<>(anterior);
        historial.addAll(temporadaActual);

        // Mapa "local|visitante" → partido jugado, para saber rápido si un partido ya tiene resultado
        Map<String, PartidoVista> jugados = new HashMap<>();
        for (PartidoVista partido : temporadaActual) {
            jugados.put(partido.local() + "|" + partido.visitante(), partido);
        }

        List<PartidoPredicho> partidos = new ArrayList<>();
        for (PartidoPendiente partido : pendientes) {
            // Un modelo por partido, entrenado solo con lo jugado antes de su fecha
            ModeloPoisson modelo = new ModeloPoisson(historial, partido.fecha());
            Prediccion prediccion = modelo.predecir(partido.fecha(), partido.local(), partido.visitante());
            partidos.add(new PartidoPredicho(prediccion, jugados.get(partido.local() + "|" + partido.visitante())));
        }

        return new Jornada(partidos, sinConexion, fiabilidad(actual - 1, anterior), actual - 1);
    }

    /** Backtesting de una temporada ya terminada, usando también la anterior a ella. */
    private BacktestingService.Resultado fiabilidad(int temporada, List<PartidoVista> partidosTemporada)
            throws SQLException {
        List<PartidoVista> historial = new ArrayList<>(repositorio.listarPartidos(temporada - 1));
        historial.addAll(partidosTemporada);
        return new BacktestingService().evaluar(historial, partidosTemporada);
    }
}
