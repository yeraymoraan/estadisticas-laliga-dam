package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.model.PartidoVista;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del simulador con una liga pequeña de 4 equipos (12 partidos: ida y vuelta). */
class SimulacionServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2025, 8, 16);
    /** La vuelta empieza 6 semanas después: antes de esta fecha solo se ha jugado la ida. */
    private static final LocalDate MITAD = INICIO.plusDays(42);

    @Test
    void conLaMismaSemillaSaleSiempreLoMismo() {
        SimulacionService.Resultado primera = new SimulacionService().simular(liga(), List.of(), MITAD, 2000, 7);
        SimulacionService.Resultado segunda = new SimulacionService().simular(liga(), List.of(), MITAD, 2000, 7);

        assertEquals(primera.tabla(), segunda.tabla());
    }

    @Test
    void cuentaBienLosPartidosJugadosYLosQueFaltan() {
        SimulacionService.Resultado resultado = new SimulacionService().simular(liga(), List.of(), MITAD, 100, 1);

        assertEquals(6, resultado.partidosJugados());
        assertEquals(6, resultado.partidosSimulados());
    }

    @Test
    void siempreHayExactamenteUnCampeon() {
        SimulacionService.Resultado resultado = new SimulacionService().simular(liga(), List.of(), MITAD, 2000, 3);

        double suma = 0;
        for (SimulacionService.FilaSimulacion fila : resultado.tabla()) {
            suma += fila.probCampeon();
        }
        assertEquals(1.0, suma, 0.000001);
    }

    @Test
    void elEquipoMuchoMejorSaleCampeonCasiSiempre() {
        // Se pasa también la temporada anterior, como en la aplicación. Sin ella el modelo no sabría
        // cómo juega A fuera de casa, porque en la ida de esta liga de prueba A juega siempre en casa.
        SimulacionService.Resultado resultado = new SimulacionService()
                .simular(liga(), temporadaAnterior(), MITAD, 2000, 5);

        // A gana todos los partidos de la ida 4-0: tiene que salir primero y con mucha probabilidad
        SimulacionService.FilaSimulacion primero = resultado.tabla().get(0);
        assertEquals("A", primero.equipo());
        assertTrue(primero.probCampeon() > 0.9);
        assertEquals(9, primero.puntosActuales());
    }

    @Test
    void sinPartidosPendientesLaSimulacionEsLaClasificacionReal() {
        LocalDate despues = INICIO.plusDays(200);

        SimulacionService.Resultado resultado = new SimulacionService().simular(liga(), List.of(), despues, 100, 1);

        assertEquals(0, resultado.partidosSimulados());
        SimulacionService.FilaSimulacion primero = resultado.tabla().get(0);
        assertEquals(1.0, primero.probCampeon(), 0.0);
        assertEquals(primero.puntosActuales(), primero.puntosMedios(), 0.0);
    }

    @Test
    void losGolesAlAzarTienenLaMediaEsperada() {
        // Si se sortean muchos goles con lambda 1,5, la media tiene que salir muy cerca de 1,5
        double[] probabilidades = ModeloPoisson.probabilidadesPoisson(1.5);
        Random azar = new Random(42);
        int total = 0;
        int veces = 20000;
        for (int i = 0; i < veces; i++) {
            total += SimulacionService.golesAlAzar(probabilidades, azar);
        }

        assertEquals(1.5, (double) total / veces, 0.05);
    }

    /**
     * Liga de 4 equipos. En la ida (antes de MITAD) A gana todo 4-0 y el resto empata 1-1.
     * La vuelta se juega igual, para que la temporada completa tenga los 12 partidos.
     */
    private static List<PartidoVista> liga() {
        String[] equipos = {"A", "B", "C", "D"};
        List<PartidoVista> partidos = new ArrayList<>();
        int dia = 0;
        // Ida: cada pareja una vez, con el primero de la pareja en casa
        for (int i = 0; i < equipos.length; i++) {
            for (int j = i + 1; j < equipos.length; j++) {
                partidos.add(partido(INICIO.plusDays(dia), equipos[i], equipos[j]));
                dia += 7;
            }
        }
        // Vuelta: las mismas parejas con los campos cambiados
        dia = 42;
        for (int i = 0; i < equipos.length; i++) {
            for (int j = i + 1; j < equipos.length; j++) {
                partidos.add(partido(INICIO.plusDays(dia), equipos[j], equipos[i]));
                dia += 7;
            }
        }
        return partidos;
    }

    /** Temporada anterior completa (12 partidos, en casa y fuera) en la que A también gana todo. */
    private static List<PartidoVista> temporadaAnterior() {
        String[] equipos = {"A", "B", "C", "D"};
        List<PartidoVista> partidos = new ArrayList<>();
        LocalDate inicioAnterior = INICIO.minusDays(365);
        int dia = 0;
        for (String local : equipos) {
            for (String visitante : equipos) {
                if (!local.equals(visitante)) {
                    partidos.add(partido(inicioAnterior.plusDays(dia), local, visitante));
                    dia += 7;
                }
            }
        }
        return partidos;
    }

    private static PartidoVista partido(LocalDate fecha, String local, String visitante) {
        int golesLocal = 1;
        int golesVisitante = 1;
        if (local.equals("A")) {
            golesLocal = 4;
            golesVisitante = 0;
        } else if (visitante.equals("A")) {
            golesLocal = 0;
            golesVisitante = 4;
        }
        return new PartidoVista(fecha, local, visitante, golesLocal, golesVisitante,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
