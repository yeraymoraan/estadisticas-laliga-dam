package es.fraydiegotadeo.laliga.model;

import java.time.LocalDate;

/** Un partido real terminado. Los valores Integer opcionales son NULL cuando no hay dato. */
public record PartidoHistorico(
        LocalDate fecha, String local, String visitante, int golesLocal, int golesVisitante,
        Integer rematesLocal, Integer rematesVisitante, Integer rematesPuertaLocal,
        Integer rematesPuertaVisitante, Integer cornersLocal, Integer cornersVisitante,
        Integer amarillasLocal, Integer amarillasVisitante, Integer rojasLocal,
        Integer rojasVisitante, Integer faltasLocal, Integer faltasVisitante) {

    public PartidoHistorico {
        if (fecha == null || local == null || visitante == null
                || local.isBlank() || visitante.isBlank() || local.equalsIgnoreCase(visitante)) {
            throw new IllegalArgumentException("Fecha y dos equipos diferentes son obligatorios");
        }
        if (golesLocal < 0 || golesVisitante < 0) {
            throw new IllegalArgumentException("Los goles no pueden ser negativos");
        }
        for (Integer dato : new Integer[] {rematesLocal, rematesVisitante,
                rematesPuertaLocal, rematesPuertaVisitante, cornersLocal,
                cornersVisitante, amarillasLocal, amarillasVisitante,
                rojasLocal, rojasVisitante, faltasLocal, faltasVisitante}) {
            if (dato != null && dato < 0) {
                throw new IllegalArgumentException("Una estadística no puede ser negativa");
            }
        }
    }
}
