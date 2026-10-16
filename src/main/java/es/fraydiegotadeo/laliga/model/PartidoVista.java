package es.fraydiegotadeo.laliga.model;

import java.time.LocalDate;

/** Valores usados para la tabla JavaFX; NULL significa estadística no disponible. */
public record PartidoVista(
        LocalDate fecha, String local, String visitante, int golesLocal, int golesVisitante,
        Integer rematesLocal, Integer rematesVisitante, Integer rematesPuertaLocal,
        Integer rematesPuertaVisitante, Integer cornersLocal, Integer cornersVisitante,
        Integer amarillasLocal, Integer amarillasVisitante, Integer rojasLocal,
        Integer rojasVisitante, Integer faltasLocal, Integer faltasVisitante) {
    public String resultado() { return golesLocal + " - " + golesVisitante; }
    public static String par(Integer a, Integer b) {
        return a == null || b == null ? "N/D" : a + " - " + b;
    }
    public String remates() { return par(rematesLocal, rematesVisitante); }
    public String rematesPuerta() { return par(rematesPuertaLocal, rematesPuertaVisitante); }
    public String corners() { return par(cornersLocal, cornersVisitante); }
    public String amarillas() { return par(amarillasLocal, amarillasVisitante); }
    public String rojas() { return par(rojasLocal, rojasVisitante); }
    public String faltas() { return par(faltasLocal, faltasVisitante); }
}
