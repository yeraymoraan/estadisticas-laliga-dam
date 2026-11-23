package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.model.PartidoVista;

import java.util.List;

/**
 * Comprueba si el modelo de Poisson funciona prediciendo partidos que ya se han jugado
 * (backtesting) y comparando con lo que pasó de verdad.
 *
 * Cada partido se predice como si fuera el día antes: el modelo solo ve los partidos anteriores
 * a su fecha. Así la prueba es justa y no hay "fuga de información" del futuro.
 *
 * Para saber si el modelo aporta algo se compara con un modelo base muy simple, que da a todos
 * los partidos las mismas probabilidades: el porcentaje de victorias locales, empates y
 * victorias visitantes de los partidos anteriores.
 */
public final class BacktestingService {

    /**
     * Resultado del backtesting.
     *
     * Brier score: para cada partido se suma (probabilidad dada - lo que pasó)² de 1, X y 2,
     * donde "lo que pasó" vale 1 para el resultado real y 0 para los otros. Después se hace la media.
     * 0 sería acertar siempre con un 100 % de seguridad; cuanto más bajo, mejor.
     *
     * @param partidos      partidos evaluados
     * @param aciertos      veces que el resultado más probable (1, X o 2) fue el real
     * @param aciertosBase  aciertos del modelo base (siempre elige el resultado más frecuente)
     * @param brierModelo   Brier score medio del modelo de Poisson
     * @param brierBase     Brier score medio del modelo base
     * @param errorGoles    error medio en goles totales: |goles esperados - goles reales|
     */
    public record Resultado(int partidos, int aciertos, int aciertosBase,
                            double brierModelo, double brierBase, double errorGoles) {

        public double porcentajeAciertos() {
            return (double) aciertos / partidos;
        }

        public double porcentajeAciertosBase() {
            return (double) aciertosBase / partidos;
        }
    }

    /**
     * Evalúa el modelo con todos los partidos de una temporada.
     *
     * @param historial  todos los partidos que el modelo puede usar (temporada anterior y la evaluada)
     * @param aEvaluar   partidos que se van a predecir y comprobar
     * @return el resultado, o null si no se ha podido evaluar ningún partido
     */
    public Resultado evaluar(List<PartidoVista> historial, List<PartidoVista> aEvaluar) {
        int partidos = 0;
        int aciertos = 0;
        int aciertosBase = 0;
        double sumaBrier = 0;
        double sumaBrierBase = 0;
        double sumaErrorGoles = 0;

        for (PartidoVista partido : aEvaluar) {
            // Modelo nuevo para cada partido, entrenado solo con lo anterior a su fecha
            ModeloPoisson modelo = new ModeloPoisson(historial, partido.fecha());
            if (modelo.partidosUsados() == 0) {
                continue; // El primer día no hay nada con qué predecir
            }
            Prediccion prediccion = modelo.predecir(partido.fecha(), partido.local(), partido.visitante());
            String real = signoReal(partido);

            if (prediccion.signoMasProbable().equals(real)) {
                aciertos++;
            }
            sumaBrier += brier(prediccion.probLocal(), prediccion.probEmpate(), prediccion.probVisitante(), real);

            double[] base = frecuenciasAnteriores(historial, partido);
            if (signoMasFrecuente(base).equals(real)) {
                aciertosBase++;
            }
            sumaBrierBase += brier(base[0], base[1], base[2], real);

            double golesEsperados = prediccion.golesLocal() + prediccion.golesVisitante();
            double golesReales = partido.golesLocal() + partido.golesVisitante();
            sumaErrorGoles += Math.abs(golesEsperados - golesReales);
            partidos++;
        }

        if (partidos == 0) {
            return null;
        }
        return new Resultado(partidos, aciertos, aciertosBase,
                sumaBrier / partidos, sumaBrierBase / partidos, sumaErrorGoles / partidos);
    }

    /** "1" si ganó el local, "X" si empataron y "2" si ganó el visitante. */
    static String signoReal(PartidoVista partido) {
        if (partido.golesLocal() > partido.golesVisitante()) {
            return "1";
        }
        if (partido.golesLocal() == partido.golesVisitante()) {
            return "X";
        }
        return "2";
    }

    /** Brier score de un partido (ver la explicación en Resultado). */
    static double brier(double probLocal, double probEmpate, double probVisitante, String real) {
        double local = real.equals("1") ? 1 : 0;
        double empate = real.equals("X") ? 1 : 0;
        double visitante = real.equals("2") ? 1 : 0;
        return Math.pow(probLocal - local, 2)
                + Math.pow(probEmpate - empate, 2)
                + Math.pow(probVisitante - visitante, 2);
    }

    /**
     * Modelo base: porcentaje de 1, X y 2 en los partidos anteriores al que se evalúa.
     *
     * @return array {probabilidad 1, probabilidad X, probabilidad 2}
     */
    private static double[] frecuenciasAnteriores(List<PartidoVista> historial, PartidoVista partido) {
        int locales = 0;
        int empates = 0;
        int visitantes = 0;
        for (PartidoVista anterior : historial) {
            if (!anterior.fecha().isBefore(partido.fecha())) {
                continue;
            }
            String signo = signoReal(anterior);
            if (signo.equals("1")) {
                locales++;
            } else if (signo.equals("X")) {
                empates++;
            } else {
                visitantes++;
            }
        }
        double total = locales + empates + visitantes;
        return new double[] {locales / total, empates / total, visitantes / total};
    }

    private static String signoMasFrecuente(double[] probabilidades) {
        if (probabilidades[0] >= probabilidades[1] && probabilidades[0] >= probabilidades[2]) {
            return "1";
        }
        if (probabilidades[2] >= probabilidades[1]) {
            return "2";
        }
        return "X";
    }
}
