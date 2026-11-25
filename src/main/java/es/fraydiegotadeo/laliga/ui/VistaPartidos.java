package es.fraydiegotadeo.laliga.ui;

import es.fraydiegotadeo.laliga.model.NombresEquipos;
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
        Tablas.agregarColumna(tabla, "FECHA", partido -> Tablas.fecha(partido.fecha()), 100);
        Tablas.agregarColumna(tabla, "LOCAL", partido -> NombresEquipos.completo(partido.local()), 175);
        Tablas.agregarColumnaCentrada(tabla, "RESULTADO", PartidoVista::resultado, 95)
                .getStyleClass().add("celda-negrita");
        Tablas.agregarColumna(tabla, "VISITANTE", partido -> NombresEquipos.completo(partido.visitante()), 175);
        Tablas.agregarColumnaCentrada(tabla, "REMATES", PartidoVista::remates, 90);
        Tablas.agregarColumnaCentrada(tabla, "A PUERTA", PartidoVista::rematesPuerta, 90);
        Tablas.agregarColumnaCentrada(tabla, "CÓRNERS", PartidoVista::corners, 90);
        Tablas.agregarColumnaCentrada(tabla, "AMARILLAS", PartidoVista::amarillas, 90);
        Tablas.agregarColumnaCentrada(tabla, "ROJAS", PartidoVista::rojas, 80);
        Tablas.agregarColumnaCentrada(tabla, "FALTAS", PartidoVista::faltas, 85);
        tabla.setPlaceholder(new Label("Cargando partidos..."));
        Tablas.ajustarAlAncho(tabla);

        buscador.setPromptText("Buscar equipo...");
        buscador.getStyleClass().add("buscador");
        buscador.setMaxWidth(320);
        // Listener: cada vez que cambia el texto del buscador se vuelve a filtrar la tabla
        buscador.textProperty().addListener((propiedad, textoAnterior, textoNuevo) -> filtrar());

        raiz = new VBox(12, buscador, tabla);
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
            // Se busca en el nombre completo ("Atlético") y en el de Football-Data ("Ath Madrid")
            String local = partido.local() + " " + NombresEquipos.completo(partido.local());
            String visitante = partido.visitante() + " " + NombresEquipos.completo(partido.visitante());
            if (local.toLowerCase().contains(filtro) || visitante.toLowerCase().contains(filtro)) {
                visibles.add(partido);
            }
        }
        tabla.setItems(FXCollections.observableArrayList(visibles));
    }
}
