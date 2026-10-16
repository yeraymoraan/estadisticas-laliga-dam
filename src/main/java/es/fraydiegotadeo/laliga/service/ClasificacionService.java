package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.model.PartidoVista;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Clasificación de trabajo: puntos, diferencia de goles, goles a favor. No aplica desempates oficiales H2H. */
public final class ClasificacionService {
    public record Fila(String equipo, int jugados, int victorias, int empates,
                       int derrotas, int golesFavor, int golesContra, int puntos) {
        public int diferencia() { return golesFavor - golesContra; }
    }

    private static final class Acumulado {
        final String equipo;
        int pj, pg, pe, pp, gf, gc, puntos;
        Acumulado(String equipo) { this.equipo = equipo; }
        void agregar(int favor, int contra) {
            pj++; gf += favor; gc += contra;
            if (favor > contra) { pg++; puntos += 3; }
            else if (favor == contra) { pe++; puntos++; }
            else pp++;
        }
        Fila fila() { return new Fila(equipo,pj,pg,pe,pp,gf,gc,puntos); }
    }

    public List<Fila> calcular(List<PartidoVista> partidos) {
        Map<String, Acumulado> mapa = new HashMap<>();
        for (PartidoVista p : partidos) {
            mapa.computeIfAbsent(p.local(), Acumulado::new).agregar(p.golesLocal(), p.golesVisitante());
            mapa.computeIfAbsent(p.visitante(), Acumulado::new).agregar(p.golesVisitante(), p.golesLocal());
        }
        List<Fila> filas = new ArrayList<>();
        mapa.values().forEach(e -> filas.add(e.fila()));
        filas.sort(Comparator.comparingInt(Fila::puntos).reversed()
                .thenComparing(Comparator.comparingInt(Fila::diferencia).reversed())
                .thenComparing(Comparator.comparingInt(Fila::golesFavor).reversed())
                .thenComparing(Fila::equipo));
        return filas;
    }
}
