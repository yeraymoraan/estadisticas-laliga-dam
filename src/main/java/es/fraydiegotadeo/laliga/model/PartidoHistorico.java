package es.fraydiegotadeo.laliga.model;

import java.time.LocalDate;

/**
 * Partido real ya terminado, tal y como se lee del archivo CSV antes de guardarlo en MySQL.
 *
 * Es un "record": Java crea automáticamente el constructor, los métodos para leer cada dato
 * (por ejemplo fecha() o golesLocal()), equals, hashCode y toString. Sus datos no se pueden
 * modificar una vez creado el objeto.
 *
 * Las estadísticas opcionales son Integer (y no int) porque pueden valer null. Un null
 * significa "la fuente no tiene ese dato", que no es lo mismo que 0.
 */
public record PartidoHistorico(
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

    /**
     * Constructor compacto del record: se ejecuta al crear el objeto y lo usamos para validar.
     * Si algún dato no es correcto se lanza IllegalArgumentException y el partido no se crea.
     */
    public PartidoHistorico {
        // La fecha y los dos equipos son obligatorios
        if (fecha == null || local == null || visitante == null
                || local.isBlank() || visitante.isBlank()) {
            throw new IllegalArgumentException("Fecha y dos equipos diferentes son obligatorios");
        }
        // Un equipo no puede jugar contra sí mismo
        if (local.equalsIgnoreCase(visitante)) {
            throw new IllegalArgumentException("Fecha y dos equipos diferentes son obligatorios");
        }
        if (golesLocal < 0 || golesVisitante < 0) {
            throw new IllegalArgumentException("Los goles no pueden ser negativos");
        }

        // Las estadísticas pueden faltar (null), pero si existen no pueden ser negativas
        Integer[] estadisticas = {rematesLocal, rematesVisitante, rematesPuertaLocal,
                rematesPuertaVisitante, cornersLocal, cornersVisitante, amarillasLocal,
                amarillasVisitante, rojasLocal, rojasVisitante, faltasLocal, faltasVisitante};
        for (Integer dato : estadisticas) {
            if (dato != null && dato < 0) {
                throw new IllegalArgumentException("Una estadística no puede ser negativa");
            }
        }
    }
}
