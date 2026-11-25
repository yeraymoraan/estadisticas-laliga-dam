package es.fraydiegotadeo.laliga.ui;

import es.fraydiegotadeo.laliga.service.ClasificacionService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/** Métodos de ayuda para montar las tablas de las distintas pantallas. */
public final class Tablas {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Tablas() {
    }

    /**
     * Hace que las columnas se repartan todo el ancho de la tabla (sin hueco a la derecha ni
     * barra de desplazamiento horizontal). Los anchos de las columnas pasan a ser proporciones.
     */
    public static void ajustarAlAncho(TableView<?> tabla) {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    /**
     * Añade una columna de texto a una tabla. Es genérico (<T>) para servir para cualquier tabla.
     *
     * @param tabla  tabla a la que se añade la columna
     * @param nombre título de la columna
     * @param texto  función que, dado un objeto de la fila, devuelve el texto a mostrar
     * @param ancho  ancho preferido en píxeles
     * @return la columna creada, por si hay que darle algún estilo
     */
    public static <T> TableColumn<T, String> agregarColumna(TableView<T> tabla, String nombre,
                                                            Function<T, String> texto, double ancho) {
        TableColumn<T, String> columna = new TableColumn<>(nombre);
        // Para cada celda, JavaFX nos da el objeto de esa fila y le aplicamos la función
        columna.setCellValueFactory(celda -> new ReadOnlyStringWrapper(texto.apply(celda.getValue())));
        columna.setPrefWidth(ancho);
        tabla.getColumns().add(columna);
        return columna;
    }

    /** Igual que agregarColumna, pero con el texto centrado (para números cortos). */
    public static <T> TableColumn<T, String> agregarColumnaCentrada(TableView<T> tabla, String nombre,
                                                                    Function<T, String> texto, double ancho) {
        TableColumn<T, String> columna = agregarColumna(tabla, nombre, texto, ancho);
        columna.getStyleClass().add("celda-centrada");
        return columna;
    }

    /** Fecha en formato español: 22/11/2026. */
    public static String fecha(LocalDate dia) {
        return dia.format(FORMATO_FECHA);
    }

    /**
     * Columna "#" con el puesto dentro de un cuadradito del color de su zona (Champions, Europa
     * League, Conference o descenso). La usan la clasificación y la simulación.
     *
     * Para dibujar algo distinto de un texto hay que crear la celda a mano con setCellFactory.
     * JavaFX reutiliza las celdas al hacer scroll, por eso en updateItem siempre se vuelve a
     * poner (o quitar) el contenido según la fila que toque. El puesto sale del número de fila,
     * así que la tabla no se debe poder reordenar.
     */
    public static <T> TableColumn<T, String> crearColumnaPosicion() {
        TableColumn<T, String> columna = new TableColumn<>("#");
        columna.setPrefWidth(52);
        columna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String valor, boolean vacia) {
                super.updateItem(valor, vacia);
                if (vacia || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                int posicion = getIndex() + 1;
                int equipos = getTableView().getItems().size();
                Label numero = new Label(String.valueOf(posicion));
                numero.getStyleClass().add("posicion");
                String estilo = claseZona(ClasificacionService.zona(posicion, equipos));
                if (estilo != null) {
                    numero.getStyleClass().add(estilo);
                }
                setGraphic(numero);
            }
        });
        return columna;
    }

    /** Clase CSS de cada zona (null si no tiene color). */
    private static String claseZona(ClasificacionService.Zona zona) {
        switch (zona) {
            case CHAMPIONS:
                return "zona-champions";
            case EUROPA_LEAGUE:
                return "zona-europa";
            case CONFERENCE_LEAGUE:
                return "zona-conference";
            case DESCENSO:
                return "zona-descenso";
            default:
                return null;
        }
    }

    /** Leyenda con el significado de cada color. */
    public static HBox crearLeyendaZonas() {
        HBox leyenda = new HBox(
                elementoLeyenda("zona-champions", "Champions League"),
                elementoLeyenda("zona-europa", "Europa League"),
                elementoLeyenda("zona-conference", "Conference League"),
                elementoLeyenda("zona-descenso", "Descenso"));
        leyenda.getStyleClass().add("leyenda");
        return leyenda;
    }

    private static HBox elementoLeyenda(String claseColor, String texto) {
        Region punto = new Region();
        punto.getStyleClass().addAll("leyenda-punto", claseColor);
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("texto-suave");
        HBox elemento = new HBox(6, punto, etiqueta);
        elemento.setStyle("-fx-alignment: center-left;");
        return elemento;
    }
}
