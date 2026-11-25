package es.fraydiegotadeo.laliga.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NombresEquiposTest {

    @Test
    void traduceLosNombresAbreviados() {
        assertEquals("Atlético de Madrid", NombresEquipos.completo("Ath Madrid"));
        assertEquals("Real Sociedad", NombresEquipos.completo("Sociedad"));
    }

    @Test
    void dejaIgualLosQueYaEstanCompletos() {
        assertEquals("Barcelona", NombresEquipos.completo("Barcelona"));
        assertEquals("Equipo nuevo", NombresEquipos.completo("Equipo nuevo"));
    }
}
