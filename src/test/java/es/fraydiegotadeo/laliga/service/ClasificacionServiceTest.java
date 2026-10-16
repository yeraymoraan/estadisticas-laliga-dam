package es.fraydiegotadeo.laliga.service;

import es.fraydiegotadeo.laliga.model.PartidoVista;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ClasificacionServiceTest {
    @Test void calculaPuntosYBalance() {
        var dia = LocalDate.of(2025,8,16);
        var encuentros = List.of(
            new PartidoVista(dia,"A","B",2,1,null,null,null,null,null,null,null,null,null,null,null,null),
            new PartidoVista(dia.plusDays(1),"B","A",0,0,null,null,null,null,null,null,null,null,null,null,null,null)
        );
        var tabla = new ClasificacionService().calcular(encuentros);
        assertEquals("A",tabla.get(0).equipo());
        assertEquals(4,tabla.get(0).puntos());
        assertEquals(1,tabla.get(0).diferencia());
        assertEquals(1,tabla.get(1).puntos());
    }
}
