package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.model.PartidoVista;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula la clasificación de una temporada a partir de sus partidos (capa service).
 *
 * Victoria = 3 puntos, empate = 1, derrota = 0.
 * Orden: puntos, diferencia de goles, goles a favor y, por último, nombre del equipo.
 * LIMITACIÓN: LaLiga desempata primero por los enfrentamientos directos entre los equipos
 * empatados; ese criterio todavía no está implementado.
 */
public final class ClasificacionService {

    private static final int PUNTOS_VICTORIA = 3;
    private static final int PUNTOS_EMPATE = 1;

    /**
     * Zona de la clasificación según el puesto. Es un enum: un tipo con un número fijo de valores.
     * Reparto habitual de LaLiga: 1-4 Champions, 5 Europa League, 6 Conference League y los 3
     * últimos descienden. Puede variar un poco cada año según quién gane la Copa del Rey.
     */
    public enum Zona {
        CHAMPIONS, EUROPA_LEAGUE, CONFERENCE_LEAGUE, NINGUNA, DESCENSO
    }

    /**
     * @param posicion puesto en la clasificación, empezando en 1
     * @param equipos  número de equipos de la liga (20 ahora; 22 en 1995/96 y 1996/97)
     * @return la zona de ese puesto
     */
    public static Zona zona(int posicion, int equipos) {
        if (posicion <= 4) {
            return Zona.CHAMPIONS;
        }
        if (posicion == 5) {
            return Zona.EUROPA_LEAGUE;
        }
        if (posicion == 6) {
            return Zona.CONFERENCE_LEAGUE;
        }
        if (posicion > equipos - 3) {
            return Zona.DESCENSO;
        }
        return Zona.NINGUNA;
    }

    /** Una fila de la tabla de clasificación. */
    public record Fila(String equipo, int jugados, int victorias, int empates,
                       int derrotas, int golesFavor, int golesContra, int puntos) {

        public int diferencia() {
            return golesFavor - golesContra;
        }
    }

    /**
     * Clase auxiliar que va sumando los datos de un equipo mientras se recorren los partidos.
     * A diferencia del record Fila, sus campos sí se pueden modificar.
     */
    private static final class Acumulado {
        private final String equipo;
        private int jugados;
        private int victorias;
        private int empates;
        private int derrotas;
        private int golesFavor;
        private int golesContra;
        private int puntos;

        Acumulado(String equipo) {
            this.equipo = equipo;
        }

        /** Suma un partido desde el punto de vista de este equipo. */
        void sumarPartido(int golesMarcados, int golesRecibidos) {
            jugados++;
            golesFavor += golesMarcados;
            golesContra += golesRecibidos;

            if (golesMarcados > golesRecibidos) {
                victorias++;
                puntos += PUNTOS_VICTORIA;
            } else if (golesMarcados == golesRecibidos) {
                empates++;
                puntos += PUNTOS_EMPATE;
            } else {
                derrotas++;
            }
        }

        Fila aFila() {
            return new Fila(equipo, jugados, victorias, empates, derrotas, golesFavor, golesContra, puntos);
        }
    }

    /**
     * @param partidos partidos terminados de una temporada
     * @return clasificación ordenada del primero al último
     */
    public List<Fila> calcular(List<PartidoVista> partidos) {
        // Mapa nombre del equipo → datos acumulados, para encontrar cada equipo rápidamente
        Map<String, Acumulado> equipos = new HashMap<>();

        for (PartidoVista partido : partidos) {
            // Cada partido cuenta para los dos equipos, con los goles "al revés" para el visitante
            obtenerAcumulado(equipos, partido.local())
                    .sumarPartido(partido.golesLocal(), partido.golesVisitante());
            obtenerAcumulado(equipos, partido.visitante())
                    .sumarPartido(partido.golesVisitante(), partido.golesLocal());
        }

        List<Fila> clasificacion = new ArrayList<>();
        for (Acumulado acumulado : equipos.values()) {
            clasificacion.add(acumulado.aFila());
        }

        // Comparator encadenado: si empatan en un criterio se mira el siguiente.
        // reversed() ordena de mayor a menor (más puntos primero).
        // Fila::puntos es una referencia a método: equivale a la lambda fila -> fila.puntos()
        clasificacion.sort(Comparator.comparingInt(Fila::puntos).reversed()
                .thenComparing(Comparator.comparingInt(Fila::diferencia).reversed())
                .thenComparing(Comparator.comparingInt(Fila::golesFavor).reversed())
                .thenComparing(Fila::equipo));
        return clasificacion;
    }

    /** Devuelve el acumulado del equipo, creándolo si es la primera vez que aparece. */
    private static Acumulado obtenerAcumulado(Map<String, Acumulado> equipos, String nombre) {
        Acumulado acumulado = equipos.get(nombre);
        if (acumulado == null) {
            acumulado = new Acumulado(nombre);
            equipos.put(nombre, acumulado);
        }
        return acumulado;
    }
}
