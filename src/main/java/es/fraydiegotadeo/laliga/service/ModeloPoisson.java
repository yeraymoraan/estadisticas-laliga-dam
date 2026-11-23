package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.model.PartidoVista;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de Poisson para predecir un partido de LaLiga a partir de los partidos anteriores.
 *
 * La distribución de Poisson sirve para contar cuántas veces pasa algo que ocurre "de vez en
 * cuando" en un tiempo fijo, como los goles en 90 minutos. Solo necesita un número, lambda
 * (la media esperada), y con él da la probabilidad de 0, 1, 2... goles:
 *
 *     P(k goles) = e^(-lambda) · lambda^k / k!
 *
 * Lambda de cada equipo se calcula con MediasEquipos (ataque, defensa y ventaja de jugar en casa).
 * Juntando las probabilidades de los dos equipos sale la de cada marcador y, sumando, la de
 * 1, X y 2. Se supone que los goles de un equipo no dependen de los del otro.
 *
 * IMPORTANTE: el modelo solo usa partidos anteriores a la fecha del partido que se predice.
 * Si usara partidos posteriores estaría "mirando el futuro" y las pruebas saldrían mejor de lo real.
 */
public final class ModeloPoisson {

    /** A partir de 10 goles la probabilidad es prácticamente 0, así que no se calcula más. */
    private static final int MAX_GOLES = 10;

    private final MediasEquipos goles;
    private final MediasEquipos corners;
    private final MediasEquipos amarillas;
    private final MediasEquipos remates;
    private final MediasEquipos faltas;
    private final int partidosUsados;

    /**
     * Prepara el modelo con los partidos jugados ANTES de una fecha.
     *
     * @param historial    partidos ya jugados (por ejemplo, la temporada actual y la anterior)
     * @param fechaPartido fecha del partido que se quiere predecir; se descartan los de ese día o posteriores
     */
    public ModeloPoisson(List<PartidoVista> historial, LocalDate fechaPartido) {
        List<PartidoVista> anteriores = new ArrayList<>();
        for (PartidoVista partido : historial) {
            if (partido.fecha().isBefore(fechaPartido)) {
                anteriores.add(partido);
            }
        }
        partidosUsados = anteriores.size();

        // PartidoVista::golesLocal es una referencia a método: le dice a MediasEquipos qué dato leer
        goles = new MediasEquipos(anteriores, PartidoVista::golesLocal, PartidoVista::golesVisitante);
        corners = new MediasEquipos(anteriores, PartidoVista::cornersLocal, PartidoVista::cornersVisitante);
        amarillas = new MediasEquipos(anteriores, PartidoVista::amarillasLocal, PartidoVista::amarillasVisitante);
        remates = new MediasEquipos(anteriores, PartidoVista::rematesLocal, PartidoVista::rematesVisitante);
        faltas = new MediasEquipos(anteriores, PartidoVista::faltasLocal, PartidoVista::faltasVisitante);
    }

    /** @return cuántos partidos anteriores ha podido usar el modelo */
    public int partidosUsados() {
        return partidosUsados;
    }

    /**
     * Predice un partido.
     *
     * @param fecha     día del partido (solo se guarda en el resultado)
     * @param local     equipo de casa
     * @param visitante equipo de fuera
     * @return la predicción
     * @throws IllegalStateException si no hay ningún partido anterior con el que calcular
     */
    public Prediccion predecir(LocalDate fecha, String local, String visitante) {
        if (!goles.hayDatos()) {
            throw new IllegalStateException("No hay partidos anteriores para hacer la predicción.");
        }
        double lambdaLocal = goles.esperadoLocal(local, visitante);
        double lambdaVisitante = goles.esperadoVisitante(local, visitante);

        double[] probLocal = probabilidadesPoisson(lambdaLocal);
        double[] probVisitante = probabilidadesPoisson(lambdaVisitante);

        // Recorremos todos los marcadores posibles (0-0, 0-1, ..., 10-10).
        // Como suponemos que los goles de cada equipo son independientes, P(i - j) = P(i) · P(j).
        double ganaLocal = 0;
        double empate = 0;
        double ganaVisitante = 0;
        double masDeDos = 0;
        double mejorProbabilidad = -1;
        String mejorMarcador = "";

        for (int i = 0; i <= MAX_GOLES; i++) {
            for (int j = 0; j <= MAX_GOLES; j++) {
                double probMarcador = probLocal[i] * probVisitante[j];
                if (i > j) {
                    ganaLocal += probMarcador;
                } else if (i == j) {
                    empate += probMarcador;
                } else {
                    ganaVisitante += probMarcador;
                }
                if (i + j >= 3) {
                    masDeDos += probMarcador;
                }
                if (probMarcador > mejorProbabilidad) {
                    mejorProbabilidad = probMarcador;
                    mejorMarcador = i + " - " + j;
                }
            }
        }

        // Al cortar en 10 goles se pierde una parte mínima de probabilidad; dividimos por el total
        // para que 1 + X + 2 sume exactamente 100 %
        double total = ganaLocal + empate + ganaVisitante;

        return new Prediccion(fecha, local, visitante, lambdaLocal, lambdaVisitante,
                ganaLocal / total, empate / total, ganaVisitante / total, masDeDos / total, mejorMarcador,
                esperadoLocal(corners, local, visitante), esperadoVisitante(corners, local, visitante),
                esperadoLocal(amarillas, local, visitante), esperadoVisitante(amarillas, local, visitante),
                esperadoLocal(remates, local, visitante), esperadoVisitante(remates, local, visitante),
                esperadoLocal(faltas, local, visitante), esperadoVisitante(faltas, local, visitante));
    }

    /**
     * Probabilidad de marcar 0, 1, 2... MAX_GOLES goles con una media lambda.
     *
     * En vez de calcular potencias y factoriales grandes se usa que cada término sale del anterior:
     * P(0) = e^(-lambda)  y  P(k) = P(k-1) · lambda / k
     *
     * @param lambda goles esperados
     * @return array donde la posición k es la probabilidad de marcar k goles
     */
    static double[] probabilidadesPoisson(double lambda) {
        double[] probabilidades = new double[MAX_GOLES + 1];
        probabilidades[0] = Math.exp(-lambda);
        for (int k = 1; k <= MAX_GOLES; k++) {
            probabilidades[k] = probabilidades[k - 1] * lambda / k;
        }
        return probabilidades;
    }

    /** Valor esperado del local para una estadística, o null si la fuente no tiene ese dato. */
    private static Double esperadoLocal(MediasEquipos estadistica, String local, String visitante) {
        if (!estadistica.hayDatos()) {
            return null;
        }
        return estadistica.esperadoLocal(local, visitante);
    }

    /** Valor esperado del visitante para una estadística, o null si la fuente no tiene ese dato. */
    private static Double esperadoVisitante(MediasEquipos estadistica, String local, String visitante) {
        if (!estadistica.hayDatos()) {
            return null;
        }
        return estadistica.esperadoVisitante(local, visitante);
    }
}
