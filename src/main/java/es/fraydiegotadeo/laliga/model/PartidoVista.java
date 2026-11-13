package es.fraydiegotadeo.laliga.model;

import java.time.LocalDate;

/**
 * Partido leído de MySQL, preparado para mostrarse en la tabla de la interfaz.
 *
 * Contiene los nombres de los equipos (no sus id) y métodos que devuelven cada estadística
 * ya formateada como texto "local - visitante". Si falta algún dato se muestra "N/D".
 */
public record PartidoVista(
        LocalDate fecha,
        String local,
        String visitante,
        int golesLocal,
        int golesVisitante,
        Integer rematesLocal,
        Integer rematesVisitante,
        Integer rematesPuertaLocal,
        Integer rematesPuertaVisitante,
        Integer cornersLocal,
        Integer cornersVisitante,
        Integer amarillasLocal,
        Integer amarillasVisitante,
        Integer rojasLocal,
        Integer rojasVisitante,
        Integer faltasLocal,
        Integer faltasVisitante) {

    /** Texto "N/D" (no disponible) que se muestra cuando la fuente no tiene un dato. */
    public static final String NO_DISPONIBLE = "N/D";

    /** Resultado del partido, por ejemplo "2 - 1". */
    public String resultado() {
        return golesLocal + " - " + golesVisitante;
    }

    /**
     * Une el dato del equipo local y el del visitante en un texto.
     *
     * @param valorLocal     dato del equipo local (puede ser null)
     * @param valorVisitante dato del equipo visitante (puede ser null)
     * @return "local - visitante", o "N/D" si falta alguno de los dos
     */
    public static String par(Integer valorLocal, Integer valorVisitante) {
        if (valorLocal == null || valorVisitante == null) {
            return NO_DISPONIBLE;
        }
        return valorLocal + " - " + valorVisitante;
    }

    public String remates() {
        return par(rematesLocal, rematesVisitante);
    }

    public String rematesPuerta() {
        return par(rematesPuertaLocal, rematesPuertaVisitante);
    }

    public String corners() {
        return par(cornersLocal, cornersVisitante);
    }

    public String amarillas() {
        return par(amarillasLocal, amarillasVisitante);
    }

    public String rojas() {
        return par(rojasLocal, rojasVisitante);
    }

    public String faltas() {
        return par(faltasLocal, faltasVisitante);
    }
}
