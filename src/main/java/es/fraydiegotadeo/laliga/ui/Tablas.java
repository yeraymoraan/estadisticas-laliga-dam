package es.fraydiegotadeo.laliga.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.function.Function;

/** Métodos de ayuda para montar las tablas de las distintas pantallas. */
public final class Tablas {

    private Tablas() {
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
}
