package es.fraydiegotadeo.laliga.model;

import java.time.LocalDate;

/** Valores usados para la tabla JavaFX; NULL significa estadística no disponible. */
public record PartidoVista(
        LocalDate fecha, String local, String visitante, int golesLocal, int golesVisitante,
        Integer rematesLocal, Integer rematesVisitante, Integer cornersLocal,
        Integer cornersVisitante, Integer amarillasLocal, Integer amarillasVisitante) {
    public String resultado() { return golesLocal + " - " + golesVisitante; }
    public static String par(Integer a, Integer b) {
        return a == null || b == null ? "N/D" : a + " - " + b;
    }
    public String remates() { return par(rematesLocal, rematesVisitante); }
    public String corners() { return par(cornersLocal, cornersVisitante); }
    public String amarillas() { return par(amarillasLocal, amarillasVisitante); }
}
