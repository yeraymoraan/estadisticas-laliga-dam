package es.fraydiegotadeo.laliga.model;

import java.time.LocalDate;

/**
 * Partido que todavía no se ha jugado (sale del calendario de la próxima jornada).
 *
 * @param fecha     día del partido
 * @param local     equipo que juega en casa
 * @param visitante equipo que juega fuera
 */
public record PartidoPendiente(LocalDate fecha, String local, String visitante) {
}
