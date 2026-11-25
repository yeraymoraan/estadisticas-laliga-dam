package es.fraydiegotadeo.laliga.ui;

import es.fraydiegotadeo.laliga.model.NombresEquipos;
import es.fraydiegotadeo.laliga.service.ClasificacionService;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Pantalla "Clasificación" de la temporada seleccionada, con las zonas europeas y de descenso. */
public final class VistaClasificacion {

    private final TableView<ClasificacionService.Fila> tabla = new TableView<>();
    private final VBox raiz;

    public VistaClasificacion() {
        tabla.getColumns().add(Tablas.crearColumnaPosicion());
        Tablas.agregarColumna(tabla, "EQUIPO", fila -> NombresEquipos.completo(fila.equipo()), 230);
        Tablas.agregarColumnaCentrada(tabla, "PJ", fila -> String.valueOf(fila.jugados()), 60);
        Tablas.agregarColumnaCentrada(tabla, "V", fila -> String.valueOf(fila.victorias()), 55);
        Tablas.agregarColumnaCentrada(tabla, "E", fila -> String.valueOf(fila.empates()), 55);
        Tablas.agregarColumnaCentrada(tabla, "D", fila -> String.valueOf(fila.derrotas()), 55);
        Tablas.agregarColumnaCentrada(tabla, "GF", fila -> String.valueOf(fila.golesFavor()), 60);
        Tablas.agregarColumnaCentrada(tabla, "GC", fila -> String.valueOf(fila.golesContra()), 60);
        Tablas.agregarColumnaCentrada(tabla, "DG", fila -> conSigno(fila.diferencia()), 60);
        Tablas.agregarColumnaCentrada(tabla, "PTS", fila -> String.valueOf(fila.puntos()), 65)
                .getStyleClass().add("celda-negrita");
        // El puesto sale del número de fila, así que no se deja reordenar la tabla pulsando en las columnas
        for (TableColumn<ClasificacionService.Fila, ?> columna : tabla.getColumns()) {
            columna.setSortable(false);
        }
        tabla.setPlaceholder(new Label("Cargando clasificación..."));
        Tablas.ajustarAlAncho(tabla);

        Label aviso = new Label("Clasificación calculada a partir de los resultados. En caso de empate a puntos se "
                + "ordena por diferencia de goles, que no es exactamente el criterio oficial de LaLiga.");
        aviso.getStyleClass().add("texto-suave");
        aviso.setWrapText(true);

        raiz = new VBox(12, tabla, Tablas.crearLeyendaZonas(), aviso);
        VBox.setVgrow(tabla, Priority.ALWAYS);
    }

    public Node getRaiz() {
        return raiz;
    }

    /** Muestra la clasificación ya calculada. */
    public void mostrar(List<ClasificacionService.Fila> filas) {
        tabla.setItems(FXCollections.observableArrayList(filas));
    }

    /** Diferencia de goles con signo: +12, 0, -5. */
    private static String conSigno(int numero) {
        if (numero > 0) {
            return "+" + numero;
        }
        return String.valueOf(numero);
    }
}
