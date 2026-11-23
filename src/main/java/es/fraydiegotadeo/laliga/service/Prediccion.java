package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.model.PartidoVista;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Resultado de predecir un partido con el modelo de Poisson.
 *
 * Los goles, córners, tarjetas y remates son valores esperados (medias), por eso llevan decimales.
 * Las estadísticas que no se pueden calcular porque no hay datos son null y se muestran como "N/D".
 *
 * @param probLocal     probabilidad de que gane el local (1), entre 0 y 1
 * @param probEmpate    probabilidad de empate (X)
 * @param probVisitante probabilidad de que gane el visitante (2)
 * @param probMasDe2    probabilidad de que haya 3 goles o más en total ("más de 2,5")
 * @param resultadoMasProbable marcador con más probabilidad, por ejemplo "1 - 0"
 */
public record Prediccion(
        LocalDate fecha,
        String local,
        String visitante,
        double golesLocal,
        double golesVisitante,
        double probLocal,
        double probEmpate,
        double probVisitante,
        double probMasDe2,
        String resultadoMasProbable,
        Double cornersLocal,
        Double cornersVisitante,
        Double amarillasLocal,
        Double amarillasVisitante,
        Double rematesLocal,
        Double rematesVisitante,
        Double faltasLocal,
        Double faltasVisitante) {

    /** Probabilidad en porcentaje sin decimales, por ejemplo 0.456 → "46 %". */
    public static String porcentaje(double probabilidad) {
        return Math.round(probabilidad * 100) + " %";
    }

    /** Une dos valores esperados en un texto "1,6 - 0,9", o "N/D" si falta alguno. */
    public static String par(Double valorLocal, Double valorVisitante) {
        if (valorLocal == null || valorVisitante == null) {
            return PartidoVista.NO_DISPONIBLE;
        }
        // Locale de España para que salga la coma decimal
        Locale espania = Locale.forLanguageTag("es-ES");
        return String.format(espania, "%.1f - %.1f", valorLocal, valorVisitante);
    }

    /** @return "1", "X" o "2" según qué resultado tiene más probabilidad */
    public String signoMasProbable() {
        if (probLocal >= probEmpate && probLocal >= probVisitante) {
            return "1";
        }
        if (probVisitante >= probEmpate) {
            return "2";
        }
        return "X";
    }

    public String goles() {
        return par(golesLocal, golesVisitante);
    }

    public String corners() {
        return par(cornersLocal, cornersVisitante);
    }

    public String amarillas() {
        return par(amarillasLocal, amarillasVisitante);
    }

    public String remates() {
        return par(rematesLocal, rematesVisitante);
    }

    public String faltas() {
        return par(faltasLocal, faltasVisitante);
    }
}
