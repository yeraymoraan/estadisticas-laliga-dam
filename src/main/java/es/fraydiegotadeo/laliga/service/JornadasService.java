package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.model.PartidoVista;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reparte los partidos de una temporada en jornadas.
 *
 * Los CSV de Football-Data no dicen la jornada de cada partido, así que se calcula: se recorren
 * los partidos por fecha y se cuenta cuántos lleva jugados cada equipo. Un partido es de la
 * jornada siguiente a la del equipo que más partidos lleva de los dos.
 * Ejemplo: si el local lleva 12 partidos y el visitante 12, el partido es de la jornada 13.
 *
 * LIMITACIÓN: un partido aplazado queda en la jornada en la que se jugó, no en la original.
 */
public final class JornadasService {

    /** Una jornada con su número y sus partidos. */
    public record Jornada(int numero, List<PartidoVista> partidos) {
    }

    private JornadasService() {
    }

    /**
     * @param partidos partidos de una temporada ordenados por fecha (como los devuelve el repositorio)
     * @return las jornadas, de la 1 a la última
     */
    public static List<Jornada> agrupar(List<PartidoVista> partidos) {
        Map<String, Integer> jugadosPorEquipo = new HashMap<>();
        // Mapa número de jornada → partidos de esa jornada
        Map<Integer, List<PartidoVista>> porJornada = new HashMap<>();
        int ultimaJornada = 0;

        for (PartidoVista partido : partidos) {
            int jugadosLocal = jugadosPorEquipo.getOrDefault(partido.local(), 0);
            int jugadosVisitante = jugadosPorEquipo.getOrDefault(partido.visitante(), 0);
            int numero = Math.max(jugadosLocal, jugadosVisitante) + 1;

            List<PartidoVista> lista = porJornada.get(numero);
            if (lista == null) {
                lista = new ArrayList<>();
                porJornada.put(numero, lista);
            }
            lista.add(partido);

            jugadosPorEquipo.put(partido.local(), jugadosLocal + 1);
            jugadosPorEquipo.put(partido.visitante(), jugadosVisitante + 1);
            ultimaJornada = Math.max(ultimaJornada, numero);
        }

        List<Jornada> jornadas = new ArrayList<>();
        for (int numero = 1; numero <= ultimaJornada; numero++) {
            List<PartidoVista> lista = porJornada.get(numero);
            if (lista != null) {
                jornadas.add(new Jornada(numero, lista));
            }
        }
        return jornadas;
    }
}
