package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.model.PartidoVista;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Medias de una estadística (goles, córners, tarjetas...) para cada equipo, separando
 * lo que hace en casa y fuera, y la media de toda la liga.
 *
 * Con ellas se calcula la "fuerza" de cada equipo:
 * - ataque = lo que hace el equipo / lo que hace un equipo medio
 * - defensa = lo que recibe el equipo / lo que recibe un equipo medio
 * Un ataque de 1,2 es un 20 % más que la media y una defensa de 0,8 es recibir un 20 % menos.
 *
 * Es package-private (sin public) porque solo la usa ModeloPoisson.
 */
final class MediasEquipos {

    /**
     * Partidos "de un equipo medio" que se le suman a cada equipo antes de calcular su media.
     * Sirve para que un equipo con pocos partidos (un recién ascendido) no tenga una fuerza
     * exagerada por una racha: con 1 partido su media se queda cerca de la de la liga, y con
     * 30 partidos manda su propio dato.
     * Se probaron 0, 3, 5, 10 y 20 con la temporada 2024/25: con 0 falla (un ascendido sin partidos
     * da 0/0), 3 y 5 dan casi el mismo resultado y 5 es el que más acierta (51,1 %).
     */
    static final int PARTIDOS_SUAVIZADO = 5;

    /** Lo que lleva acumulado un equipo en casa y fuera. */
    private static final class Acumulado {
        private double favorCasa;
        private double contraCasa;
        private int partidosCasa;
        private double favorFuera;
        private double contraFuera;
        private int partidosFuera;
    }

    private final Map<String, Acumulado> equipos = new HashMap<>();
    private double sumaLocal;
    private double sumaVisitante;
    private int partidos;

    /**
     * Recorre los partidos y va sumando la estadística de cada equipo.
     *
     * Recibe dos funciones que dicen qué dato hay que leer de cada partido. Por ejemplo, para
     * los córners: PartidoVista::cornersLocal y PartidoVista::cornersVisitante. Así la misma
     * clase sirve para goles, córners, tarjetas o remates sin repetir código.
     *
     * @param lista         partidos ya jugados
     * @param datoLocal     función que devuelve el dato del equipo local (null si no hay dato)
     * @param datoVisitante función que devuelve el dato del equipo visitante (null si no hay dato)
     */
    MediasEquipos(List<PartidoVista> lista, Function<PartidoVista, Integer> datoLocal,
                  Function<PartidoVista, Integer> datoVisitante) {
        for (PartidoVista partido : lista) {
            Integer local = datoLocal.apply(partido);
            Integer visitante = datoVisitante.apply(partido);
            // Si la fuente no tiene el dato (N/D) el partido no cuenta, nunca se toma como 0
            if (local == null || visitante == null) {
                continue;
            }

            sumaLocal += local;
            sumaVisitante += visitante;
            partidos++;

            Acumulado equipoLocal = obtener(partido.local());
            equipoLocal.favorCasa += local;
            equipoLocal.contraCasa += visitante;
            equipoLocal.partidosCasa++;

            Acumulado equipoVisitante = obtener(partido.visitante());
            equipoVisitante.favorFuera += visitante;
            equipoVisitante.contraFuera += local;
            equipoVisitante.partidosFuera++;
        }
    }

    /** @return true si hay al menos un partido con este dato */
    boolean hayDatos() {
        return partidos > 0;
    }

    /** @return media de la liga para el equipo que juega en casa */
    double mediaLocal() {
        return sumaLocal / partidos;
    }

    /** @return media de la liga para el equipo que juega fuera */
    double mediaVisitante() {
        return sumaVisitante / partidos;
    }

    /**
     * Valor esperado para el equipo local:
     * media de la liga en casa × ataque del local en casa × defensa del visitante fuera.
     */
    double esperadoLocal(String local, String visitante) {
        double media = mediaLocal();
        if (media == 0) {
            return 0;
        }
        Acumulado casa = obtener(local);
        Acumulado fuera = obtener(visitante);
        double ataque = suavizar(casa.favorCasa, casa.partidosCasa, media) / media;
        double defensa = suavizar(fuera.contraFuera, fuera.partidosFuera, media) / media;
        return media * ataque * defensa;
    }

    /**
     * Valor esperado para el equipo visitante:
     * media de la liga fuera × ataque del visitante fuera × defensa del local en casa.
     */
    double esperadoVisitante(String local, String visitante) {
        double media = mediaVisitante();
        if (media == 0) {
            return 0;
        }
        Acumulado casa = obtener(local);
        Acumulado fuera = obtener(visitante);
        double ataque = suavizar(fuera.favorFuera, fuera.partidosFuera, media) / media;
        double defensa = suavizar(casa.contraCasa, casa.partidosCasa, media) / media;
        return media * ataque * defensa;
    }

    /**
     * Media del equipo añadiendo PARTIDOS_SUAVIZADO partidos de un equipo medio.
     * Ejemplo: 10 goles en 4 partidos con media de liga 1,5 → (10 + 5·1,5) / (4 + 5) = 1,94
     * en vez de 2,5.
     */
    private static double suavizar(double suma, int numPartidos, double mediaLiga) {
        return (suma + PARTIDOS_SUAVIZADO * mediaLiga) / (numPartidos + PARTIDOS_SUAVIZADO);
    }

    /** Devuelve el acumulado del equipo; si no aparece en los datos, uno vacío (= equipo medio). */
    private Acumulado obtener(String equipo) {
        Acumulado acumulado = equipos.get(equipo);
        if (acumulado == null) {
            acumulado = new Acumulado();
            equipos.put(equipo, acumulado);
        }
        return acumulado;
    }
}
