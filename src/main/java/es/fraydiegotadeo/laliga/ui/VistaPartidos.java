package es.fraydiegotadeo.laliga.ui;

import es.fraydiegotadeo.laliga.model.PartidoVista;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/** Pantalla "Partidos": todos los partidos de una temporada con un buscador por equipo. */
public final class VistaPartidos {

    private final TableView<PartidoVista> tabla = new TableView<>();
    private final TextField buscador = new TextField();
    private final VBox raiz;

    /** Partidos de la temporada, sin filtrar. */
    private List<PartidoVista> partidos = new ArrayList<>();

    public VistaPartidos() {
        Tablas.agregarColumna(tabla, "Fecha", partido -> partido.fecha().toString(), 100);
        Tablas.agregarColumna(tabla, "Local", PartidoVista::local, 150);
        Tablas.agregarColumna(tabla, "Visitante", PartidoVista::visitante, 150);
        Tablas.agregarColumna(tabla, "Resultado", PartidoVista::resultado, 100);
        Tablas.agregarColumna(tabla, "Remates", PartidoVista::remates, 105);
        Tablas.agregarColumna(tabla, "A puerta", PartidoVista::rematesPuerta, 105);
        Tablas.agregarColumna(tabla, "Córners", PartidoVista::corners, 105);
        Tablas.agregarColumna(tabla, "Amarillas", PartidoVista::amarillas, 105);
        Tablas.agregarColumna(tabla, "Rojas", PartidoVista::rojas, 105);
        Tablas.agregarColumna(tabla, "Faltas", PartidoVista::faltas, 105);
        tabla.setPlaceholder(new Label("Cargando partidos..."));

        buscador.setPromptText("Filtrar por equipo");
        // Listener: cada vez que cambia el texto del buscador se vuelve a filtrar la tabla
        buscador.textProperty().addListener((propiedad, textoAnterior, textoNuevo) -> filtrar());

        raiz = new VBox(10, buscador, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
    }

    /** @return el contenedor de la pantalla, para ponerlo en la ventana */
    public Node getRaiz() {
        return raiz;
    }

    /** Muestra los partidos de una temporada (aplicando el filtro que haya escrito). */
    public void mostrar(List<PartidoVista> partidosTemporada) {
        partidos = partidosTemporada;
        filtrar();
    }

    /** Deja en la tabla solo los partidos del equipo escrito en el buscador. */
    private void filtrar() {
        String filtro = buscador.getText() == null ? "" : buscador.getText().trim().toLowerCase();

        List<PartidoVista> visibles = new ArrayList<>();
        for (PartidoVista partido : partidos) {
            if (partido.local().toLowerCase().contains(filtro)
                    || partido.visitante().toLowerCase().contains(filtro)) {
                visibles.add(partido);
            }
        }
        tabla.setItems(FXCollections.observableArrayList(visibles));
    }
}
