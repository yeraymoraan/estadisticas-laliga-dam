package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.model.PartidoVista;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del modelo de Poisson con ejemplos pequeños que se pueden comprobar a mano. */
class ModeloPoissonTest {

    private static final LocalDate INICIO = LocalDate.of(2025, 8, 16);

    @Test
    void formulaDePoissonCoincideConElCalculoAMano() {
        // Con lambda = 1: P(0) = e^-1 = 0,3679 y P(2) = e^-1 · 1² / 2! = 0,1839
        double[] probabilidades = ModeloPoisson.probabilidadesPoisson(1.0);

        assertEquals(0.3679, probabilidades[0], 0.0001);
        assertEquals(0.3679, probabilidades[1], 0.0001);
        assertEquals(0.1839, probabilidades[2], 0.0001);
    }

    @Test
    void lasProbabilidadesSumanCien() {
        Prediccion prediccion = new ModeloPoisson(ligaDePrueba(), INICIO.plusDays(100))
                .predecir(INICIO.plusDays(100), "A", "B");

        double suma = prediccion.probLocal() + prediccion.probEmpate() + prediccion.probVisitante();
        assertEquals(1.0, suma, 0.000001);
    }

    @Test
    void elEquipoQueMasMarcaEsFavorito() {
        // En la liga de prueba A gana siempre y B pierde siempre
        Prediccion prediccion = new ModeloPoisson(ligaDePrueba(), INICIO.plusDays(100))
                .predecir(INICIO.plusDays(100), "B", "A");

        assertTrue(prediccion.probVisitante() > prediccion.probLocal());
        assertEquals("2", prediccion.signoMasProbable());
    }

    @Test
    void noUsaPartidosDelMismoDiaNiPosteriores() {
        List<PartidoVista> historial = ligaDePrueba();
        LocalDate fecha = INICIO.plusDays(100);
        Prediccion antes = new ModeloPoisson(historial, fecha).predecir(fecha, "A", "B");

        // Se añaden partidos del futuro en los que B golea: la predicción no debe cambiar
        List<PartidoVista> conFuturo = new ArrayList<>(historial);
        conFuturo.add(partido(fecha, "B", "A", 9, 0));
        conFuturo.add(partido(fecha.plusDays(7), "B", "A", 9, 0));
        Prediccion despues = new ModeloPoisson(conFuturo, fecha).predecir(fecha, "A", "B");

        assertEquals(antes.probLocal(), despues.probLocal(), 0.0);
        assertEquals(historial.size(), new ModeloPoisson(conFuturo, fecha).partidosUsados());
    }

    @Test
    void sinCornersEnLosDatosDevuelveNoDisponible() {
        Prediccion prediccion = new ModeloPoisson(ligaDePrueba(), INICIO.plusDays(100))
                .predecir(INICIO.plusDays(100), "A", "B");

        assertNull(prediccion.cornersLocal());
        assertEquals("N/D", prediccion.corners());
        assertNotNull(prediccion.goles());
    }

    @Test
    void sinPartidosAnterioresNoSePuedePredecir() {
        ModeloPoisson modelo = new ModeloPoisson(ligaDePrueba(), INICIO);

        assertThrows(IllegalStateException.class, () -> modelo.predecir(INICIO, "A", "B"));
    }

    /** Liga de 4 equipos con 12 partidos: A es el mejor, B el peor. */
    private static List<PartidoVista> ligaDePrueba() {
        List<PartidoVista> partidos = new ArrayList<>();
        String[] equipos = {"A", "C", "D", "B"};
        int dia = 0;
        for (String local : equipos) {
            for (String visitante : equipos) {
                if (local.equals(visitante)) {
                    continue;
                }
                partidos.add(partido(INICIO.plusDays(dia), local, visitante,
                        goles(local, visitante), goles(visitante, local)));
                dia += 7;
            }
        }
        return partidos;
    }

    /** A marca 3, C y D marcan 1, B no marca nunca. */
    private static int goles(String equipo, String rival) {
        if (equipo.equals("A")) {
            return 3;
        }
        if (equipo.equals("B")) {
            return 0;
        }
        return 1;
    }

    /** Partido de prueba solo con el resultado (el resto de estadísticas a null). */
    private static PartidoVista partido(LocalDate fecha, String local, String visitante,
                                        int golesLocal, int golesVisitante) {
        return new PartidoVista(fecha, local, visitante, golesLocal, golesVisitante,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
