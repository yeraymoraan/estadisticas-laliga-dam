package es.fraydiegotadeo.laliga.ui;

import es.fraydiegotadeo.laliga.service.BacktestingService;
import es.fraydiegotadeo.laliga.service.Prediccion;
import es.fraydiegotadeo.laliga.service.PrediccionService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Pantalla "Próxima jornada": predicción de cada partido y fiabilidad del modelo. */
public final class VistaPrediccion {

    private final TableView<Prediccion> tabla = new TableView<>();
    private final Label infoModelo = new Label("Pulsa el botón para descargar el calendario y calcular la predicción.");
    private final Button botonPredecir = new Button("Descargar calendario y predecir");
    private final VBox raiz;

    public VistaPrediccion() {
        Tablas.agregarColumna(tabla, "Fecha", prediccion -> prediccion.fecha().toString(), 90);
        Tablas.agregarColumna(tabla, "Local", Prediccion::local, 115);
        Tablas.agregarColumna(tabla, "Visitante", Prediccion::visitante, 115);
        Tablas.agregarColumna(tabla, "1", prediccion -> Prediccion.porcentaje(prediccion.probLocal()), 52);
        Tablas.agregarColumna(tabla, "X", prediccion -> Prediccion.porcentaje(prediccion.probEmpate()), 52);
        Tablas.agregarColumna(tabla, "2", prediccion -> Prediccion.porcentaje(prediccion.probVisitante()), 52);
        Tablas.agregarColumna(tabla, "Pronóstico", Prediccion::signoMasProbable, 80);
        Tablas.agregarColumna(tabla, "Goles esp.", Prediccion::goles, 82);
        Tablas.agregarColumna(tabla, "Marcador", Prediccion::resultadoMasProbable, 75);
        Tablas.agregarColumna(tabla, "+2,5 goles", prediccion -> Prediccion.porcentaje(prediccion.probMasDe2()), 78);
        Tablas.agregarColumna(tabla, "Córners", Prediccion::corners, 82);
        Tablas.agregarColumna(tabla, "Amarillas", Prediccion::amarillas, 82);
        Tablas.agregarColumna(tabla, "Remates", Prediccion::remates, 85);
        Tablas.agregarColumna(tabla, "Faltas", Prediccion::faltas, 85);
        tabla.setPlaceholder(new Label("Sin predicciones todavía."));

        Label aviso = new Label("Son estimaciones del modelo de Poisson con los partidos de esta temporada "
                + "y la anterior, no resultados seguros. Los valores con decimales son medias esperadas.");
        aviso.setWrapText(true);
        infoModelo.setWrapText(true);

        raiz = new VBox(8, botonPredecir, infoModelo, aviso, tabla);
        raiz.setPadding(new Insets(8, 0, 0, 0));
        VBox.setVgrow(tabla, Priority.ALWAYS);
    }

    public Node getRaiz() {
        return raiz;
    }

    /** Botón que lanza la predicción (la ventana le asigna qué hacer). */
    public Button getBotonPredecir() {
        return botonPredecir;
    }

    /** Muestra las predicciones y la fiabilidad del modelo. */
    public void mostrar(PrediccionService.Jornada jornada) {
        tabla.setItems(FXCollections.observableArrayList(jornada.predicciones()));

        BacktestingService.Resultado resultado = jornada.fiabilidad();
        if (resultado == null) {
            infoModelo.setText("No hay datos suficientes para medir la fiabilidad del modelo.");
            return;
        }
        int temporada = jornada.temporadaEvaluada();
        infoModelo.setText("Fiabilidad: prediciendo los " + resultado.partidos() + " partidos de la temporada "
                + temporada + "/" + String.format("%02d", (temporada + 1) % 100)
                + " sin mirar el futuro, el modelo acertó el " + Prediccion.porcentaje(resultado.porcentajeAciertos())
                + " de los 1-X-2 (elegir siempre el resultado más frecuente: "
                + Prediccion.porcentaje(resultado.porcentajeAciertosBase()) + ").");
    }
}
