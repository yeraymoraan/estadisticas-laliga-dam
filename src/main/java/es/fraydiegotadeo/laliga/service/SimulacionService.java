package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.model.PartidoVista;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Simula cómo puede terminar una temporada con el método de Monte Carlo.
 *
 * Monte Carlo consiste en repetir muchas veces un experimento al azar y contar los resultados.
 * Aquí el experimento es "jugar" todos los partidos que faltan: los goles de cada equipo se sacan
 * al azar con la distribución de Poisson del modelo. Si en 10.000 temporadas simuladas un equipo
 * queda campeón 6.000 veces, su probabilidad de ser campeón es del 60 %.
 *
 * Partidos que faltan: en LaLiga cada equipo juega contra cada uno de los otros una vez en casa
 * y otra fuera, así que son todas las parejas (local, visitante) que todavía no se han jugado.
 */
public final class SimulacionService {

    /** Puestos de la clasificación que se cuentan como "Champions" (los 4 primeros). */
    private static final int PUESTOS_CHAMPIONS = 4;
    /** Puestos de descenso a Segunda (los 3 últimos). */
    private static final int PUESTOS_DESCENSO = 3;

    /**
     * Resultado de la simulación para un equipo. Las medias y probabilidades son sobre todas
     * las temporadas simuladas.
     */
    public record FilaSimulacion(String equipo, int puntosActuales, double puntosMedios,
                                 double golesFavorMedios, double diferenciaMedia, double posicionMedia,
                                 double probCampeon, double probChampions, double probDescenso) {
    }

    /** Resultado completo: la tabla ordenada y cuántos partidos se han simulado. */
    public record Resultado(List<FilaSimulacion> tabla, int partidosJugados, int partidosSimulados,
                            int simulaciones) {
    }

    /**
     * Partido que falta, con la probabilidad de marcar 0, 1, 2... goles de cada equipo.
     * Se calculan una sola vez y se reutilizan en todas las simulaciones (si no, se repetiría
     * la fórmula de Poisson millones de veces).
     */
    private record PartidoSimulado(int local, int visitante, double[] probLocal, double[] probVisitante) {
    }

    /**
     * Simula el final de una temporada.
     *
     * @param temporada            partidos de la temporada que se simula
     * @param temporadaAnterior    partidos de la temporada anterior (ayudan a conocer a los equipos)
     * @param fechaCorte           se toman como jugados los partidos anteriores a esta fecha y se simula el resto.
     *                             Para la temporada en curso basta con el día de mañana.
     * @param simulaciones         cuántas temporadas se simulan (por ejemplo 10.000)
     * @param semilla              semilla del generador de números aleatorios: con la misma semilla
     *                             sale siempre lo mismo, y así las pruebas se pueden repetir
     * @return la clasificación simulada, ordenada por puntos medios
     */
    public Resultado simular(List<PartidoVista> temporada, List<PartidoVista> temporadaAnterior,
                             LocalDate fechaCorte, int simulaciones, long semilla) {
        // 1. Separar lo ya jugado y sacar la lista de equipos de la temporada
        List<PartidoVista> jugados = new ArrayList<>();
        for (PartidoVista partido : temporada) {
            if (partido.fecha().isBefore(fechaCorte)) {
                jugados.add(partido);
            }
        }
        List<String> equipos = equiposDeLaTemporada(temporada);
        int numEquipos = equipos.size();

        // 2. Clasificación de partida, con los partidos ya jugados
        int[] puntosBase = new int[numEquipos];
        int[] favorBase = new int[numEquipos];
        int[] contraBase = new int[numEquipos];
        for (PartidoVista partido : jugados) {
            sumarResultado(equipos.indexOf(partido.local()), equipos.indexOf(partido.visitante()),
                    partido.golesLocal(), partido.golesVisitante(), puntosBase, favorBase, contraBase);
        }

        // 3. Goles esperados de cada partido que falta. El modelo solo usa partidos anteriores a la fecha de corte.
        List<PartidoVista> historial = new ArrayList<>(temporadaAnterior);
        historial.addAll(temporada);
        ModeloPoisson modelo = new ModeloPoisson(historial, fechaCorte);
        List<PartidoSimulado> pendientes = new ArrayList<>();
        for (int[] pareja : parejasPendientes(equipos, jugados)) {
            Prediccion prediccion = modelo.predecir(fechaCorte, equipos.get(pareja[0]), equipos.get(pareja[1]));
            pendientes.add(new PartidoSimulado(pareja[0], pareja[1],
                    ModeloPoisson.probabilidadesPoisson(prediccion.golesLocal()),
                    ModeloPoisson.probabilidadesPoisson(prediccion.golesVisitante())));
        }

        // 4. Simular muchas temporadas e ir acumulando los resultados de cada equipo
        Random azar = new Random(semilla);
        double[] sumaPuntos = new double[numEquipos];
        double[] sumaFavor = new double[numEquipos];
        double[] sumaDiferencia = new double[numEquipos];
        double[] sumaPosicion = new double[numEquipos];
        int[] vecesCampeon = new int[numEquipos];
        int[] vecesChampions = new int[numEquipos];
        int[] vecesDescenso = new int[numEquipos];

        for (int s = 0; s < simulaciones; s++) {
            // Cada temporada simulada empieza desde la clasificación real
            int[] puntos = puntosBase.clone();
            int[] favor = favorBase.clone();
            int[] contra = contraBase.clone();

            for (PartidoSimulado partido : pendientes) {
                int golesLocal = golesAlAzar(partido.probLocal(), azar);
                int golesVisitante = golesAlAzar(partido.probVisitante(), azar);
                sumarResultado(partido.local(), partido.visitante(), golesLocal, golesVisitante,
                        puntos, favor, contra);
            }

            List<Integer> orden = ordenarClasificacion(equipos, puntos, favor, contra);
            for (int posicion = 0; posicion < numEquipos; posicion++) {
                int equipo = orden.get(posicion);
                sumaPuntos[equipo] += puntos[equipo];
                sumaFavor[equipo] += favor[equipo];
                sumaDiferencia[equipo] += favor[equipo] - contra[equipo];
                sumaPosicion[equipo] += posicion + 1;
                if (posicion == 0) {
                    vecesCampeon[equipo]++;
                }
                if (posicion < PUESTOS_CHAMPIONS) {
                    vecesChampions[equipo]++;
                }
                if (posicion >= numEquipos - PUESTOS_DESCENSO) {
                    vecesDescenso[equipo]++;
                }
            }
        }

        // 5. Pasar las sumas a medias y probabilidades
        List<FilaSimulacion> tabla = new ArrayList<>();
        for (int i = 0; i < numEquipos; i++) {
            tabla.add(new FilaSimulacion(equipos.get(i), puntosBase[i],
                    sumaPuntos[i] / simulaciones, sumaFavor[i] / simulaciones,
                    sumaDiferencia[i] / simulaciones, sumaPosicion[i] / simulaciones,
                    (double) vecesCampeon[i] / simulaciones, (double) vecesChampions[i] / simulaciones,
                    (double) vecesDescenso[i] / simulaciones));
        }
        // Ordenar por posición media (la mejor primero)
        tabla.sort(Comparator.comparingDouble(FilaSimulacion::posicionMedia));
        return new Resultado(tabla, jugados.size(), pendientes.size(), simulaciones);
    }

    /**
     * Saca al azar los goles de un equipo con la distribución de Poisson (método de la "ruleta").
     *
     * @param probabilidades P(0), P(1), P(2)... calculadas con ModeloPoisson.probabilidadesPoisson
     * @param azar           generador de números aleatorios
     *
     * Se tira un número al azar entre 0 y 1 y se van sumando P(0), P(1), P(2)...
     * Cuando la suma pasa del número, esos son los goles. Ejemplo con lambda 1:
     * P(0)=0,37 y P(1)=0,37 → si sale 0,20 son 0 goles; si sale 0,50 es 1 gol (0,37 + 0,37 = 0,74 > 0,50).
     */
    static int golesAlAzar(double[] probabilidades, Random azar) {
        double numero = azar.nextDouble();
        double acumulada = 0;
        for (int goles = 0; goles < probabilidades.length; goles++) {
            acumulada += probabilidades[goles];
            if (numero < acumulada) {
                return goles;
            }
        }
        // Solo llega aquí en el caso rarísimo de más de 10 goles
        return probabilidades.length - 1;
    }

    /** Suma un resultado a la clasificación: 3 puntos por victoria, 1 por empate y los goles. */
    private static void sumarResultado(int local, int visitante, int golesLocal, int golesVisitante,
                                       int[] puntos, int[] favor, int[] contra) {
        favor[local] += golesLocal;
        contra[local] += golesVisitante;
        favor[visitante] += golesVisitante;
        contra[visitante] += golesLocal;
        if (golesLocal > golesVisitante) {
            puntos[local] += 3;
        } else if (golesLocal == golesVisitante) {
            puntos[local] += 1;
            puntos[visitante] += 1;
        } else {
            puntos[visitante] += 3;
        }
    }

    /**
     * Devuelve los índices de los equipos ordenados como la clasificación:
     * puntos, diferencia de goles y goles a favor (el mismo criterio que ClasificacionService).
     */
    private static List<Integer> ordenarClasificacion(List<String> equipos, int[] puntos, int[] favor, int[] contra) {
        List<Integer> orden = new ArrayList<>();
        for (int i = 0; i < equipos.size(); i++) {
            orden.add(i);
        }
        orden.sort(Comparator.comparingInt((Integer i) -> puntos[i]).reversed()
                .thenComparing(Comparator.comparingInt((Integer i) -> favor[i] - contra[i]).reversed())
                .thenComparing(Comparator.comparingInt((Integer i) -> favor[i]).reversed())
                .thenComparing(i -> equipos.get(i)));
        return orden;
    }

    /** Lista de equipos que aparecen en la temporada, sin repetir. */
    private static List<String> equiposDeLaTemporada(List<PartidoVista> temporada) {
        List<String> equipos = new ArrayList<>();
        for (PartidoVista partido : temporada) {
            if (!equipos.contains(partido.local())) {
                equipos.add(partido.local());
            }
            if (!equipos.contains(partido.visitante())) {
                equipos.add(partido.visitante());
            }
        }
        return equipos;
    }

    /**
     * Parejas {local, visitante} que todavía no se han jugado.
     * Se guardan las jugadas como texto "local|visitante" en un Set para buscarlas rápido.
     */
    private static List<int[]> parejasPendientes(List<String> equipos, List<PartidoVista> jugados) {
        Set<String> yaJugados = new HashSet<>();
        for (PartidoVista partido : jugados) {
            yaJugados.add(partido.local() + "|" + partido.visitante());
        }
        List<int[]> pendientes = new ArrayList<>();
        for (int local = 0; local < equipos.size(); local++) {
            for (int visitante = 0; visitante < equipos.size(); visitante++) {
                if (local != visitante && !yaJugados.contains(equipos.get(local) + "|" + equipos.get(visitante))) {
                    pendientes.add(new int[] {local, visitante});
                }
            }
        }
        return pendientes;
    }
}
