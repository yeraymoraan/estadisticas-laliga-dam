package es.fraydiegotadeo.laliga.ui;

import es.fraydiegotadeo.laliga.service.ClasificacionService;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Pantalla "Clasificación" de la temporada seleccionada. */
public final class VistaClasificacion {

    private final TableView<ClasificacionService.Fila> tabla = new TableView<>();
    private final VBox raiz;

    public VistaClasificacion() {
        Tablas.agregarColumna(tabla, "Equipo", ClasificacionService.Fila::equipo, 170);
        Tablas.agregarColumna(tabla, "PJ", fila -> String.valueOf(fila.jugados()), 62);
        Tablas.agregarColumna(tabla, "V", fila -> String.valueOf(fila.victorias()), 55);
        Tablas.agregarColumna(tabla, "E", fila -> String.valueOf(fila.empates()), 55);
        Tablas.agregarColumna(tabla, "D", fila -> String.valueOf(fila.derrotas()), 55);
        Tablas.agregarColumna(tabla, "GF", fila -> String.valueOf(fila.golesFavor()), 60);
        Tablas.agregarColumna(tabla, "GC", fila -> String.valueOf(fila.golesContra()), 60);
        Tablas.agregarColumna(tabla, "DG", fila -> String.valueOf(fila.diferencia()), 60);
        Tablas.agregarColumna(tabla, "Pts", fila -> String.valueOf(fila.puntos()), 65);

        Label avisoDesempate = new Label("Clasificación calculada. Desempate simplificado por diferencia "
                + "de goles; no equivale al criterio oficial de LaLiga.");
        raiz = new VBox(6, avisoDesempate, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
    }

    public Node getRaiz() {
        return raiz;
    }

    /** Muestra la clasificación ya calculada. */
    public void mostrar(List<ClasificacionService.Fila> filas) {
        tabla.setItems(FXCollections.observableArrayList(filas));
    }
}
