package es.fraydiegotadeo.laliga.ui;

import es.fraydiegotadeo.laliga.model.NombresEquipos;
import es.fraydiegotadeo.laliga.service.SimulacionService;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Locale;

/**
 * Pantalla "Simulación": cómo puede terminar la temporada según la simulación de Monte Carlo.
 * La tabla está ordenada por la posición media de cada equipo en todas las temporadas simuladas.
 */
public final class VistaSimulacion {

    private static final Locale ESPANIA = Locale.forLanguageTag("es-ES");

    private final TableView<SimulacionService.FilaSimulacion> tabla = new TableView<>();
    private final Label numeroSimulaciones = new Label("–");
    private final Label detalle = new Label("Simulando la temporada...");
    private final VBox raiz;

    public VistaSimulacion() {
        tabla.getColumns().add(Tablas.crearColumnaPosicion());
        Tablas.agregarColumna(tabla, "EQUIPO", fila -> NombresEquipos.completo(fila.equipo()), 210);
        Tablas.agregarColumnaCentrada(tabla, "PTS HOY", fila -> String.valueOf(fila.puntosActuales()), 75);
        Tablas.agregarColumnaCentrada(tabla, "PTS FINAL", fila -> decimal(fila.puntosMedios()), 85)
                .getStyleClass().add("celda-negrita");
        Tablas.agregarColumnaCentrada(tabla, "GOLES", fila -> decimal(fila.golesFavorMedios()), 70);
        Tablas.agregarColumnaCentrada(tabla, "DG", fila -> conSigno(fila.diferenciaMedia()), 70);
        Tablas.agregarColumnaCentrada(tabla, "CAMPEÓN", fila -> probabilidad(fila.probCampeon()), 85)
                .getStyleClass().add("celda-principal");
        Tablas.agregarColumnaCentrada(tabla, "CHAMPIONS", fila -> probabilidad(fila.probChampions()), 90);
        Tablas.agregarColumnaCentrada(tabla, "DESCENSO", fila -> probabilidad(fila.probDescenso()), 85);
        for (TableColumn<SimulacionService.FilaSimulacion, ?> columna : tabla.getColumns()) {
            columna.setSortable(false);
        }
        tabla.setPlaceholder(new Label("Simulando la temporada..."));
        Tablas.ajustarAlAncho(tabla);

        Label aviso = new Label("PTS FINAL, GOLES y DG son medias de todas las temporadas simuladas. "
                + "\"–\" significa que no ha pasado en ninguna simulación. El modelo no conoce fichajes, "
                + "lesiones ni cambios de entrenador.");
        aviso.getStyleClass().add("texto-suave");
        aviso.setWrapText(true);

        raiz = new VBox(16, crearResumen(), tabla, Tablas.crearLeyendaZonas(), aviso);
        VBox.setVgrow(tabla, Priority.ALWAYS);
    }

    public Node getRaiz() {
        return raiz;
    }

    /** Muestra un mensaje mientras se simula o si algo ha fallado. */
    public void mostrarMensaje(String mensaje) {
        detalle.setText(mensaje);
    }

    /** Muestra el resultado de la simulación. */
    public void mostrar(SimulacionService.Resultado resultado) {
        tabla.setItems(FXCollections.observableArrayList(resultado.tabla()));
        numeroSimulaciones.setText(String.format(ESPANIA, "%,d", resultado.simulaciones()));
        detalle.setText(resultado.partidosJugados() + " partidos jugados · " + resultado.partidosSimulados()
                + " simulados en cada temporada");
    }

    /** Tarjeta de arriba: cuántas temporadas se han simulado. */
    private Node crearResumen() {
        Label titulo = new Label("TEMPORADAS SIMULADAS");
        titulo.getStyleClass().add("resumen-titulo");
        numeroSimulaciones.getStyleClass().add("resumen-numero");
        detalle.getStyleClass().add("texto-suave");
        VBox resumen = new VBox(2, titulo, numeroSimulaciones, detalle);
        resumen.getStyleClass().add("tarjeta");
        resumen.setMaxWidth(420);
        return resumen;
    }

    /** 85.43 → "85,4". */
    private static String decimal(double numero) {
        return String.format(ESPANIA, "%.1f", numero);
    }

    /** Diferencia de goles media con signo: "+12,3" o "-4,0". */
    private static String conSigno(double numero) {
        if (numero > 0) {
            return "+" + decimal(numero);
        }
        return decimal(numero);
    }

    /**
     * Probabilidad en porcentaje. Si no ha pasado nunca se pone "–", y si es muy pequeña "<1 %",
     * para no enseñar un "0 %" que parecería imposible.
     */
    private static String probabilidad(double valor) {
        if (valor == 0) {
            return "–";
        }
        if (valor < 0.005) {
            return "<1 %";
        }
        return Math.round(valor * 100) + " %";
    }
}
