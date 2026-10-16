package es.fraydiegotadeo.laliga;

import es.fraydiegotadeo.laliga.importer.CsvPartidosParser;
import es.fraydiegotadeo.laliga.model.PartidoVista;
import es.fraydiegotadeo.laliga.repository.PartidosRepository;
import es.fraydiegotadeo.laliga.service.ClasificacionService;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Primera pantalla: importación manual de CSV y consulta de los resultados históricos. */
public final class App extends Application {
    private final PartidosRepository repo = new PartidosRepository();
    private final CsvPartidosParser parser = new CsvPartidosParser();
    private final ClasificacionService clasificador = new ClasificacionService();
    private final ComboBox<Integer> temporadas = new ComboBox<>();
    private final TextField busqueda = new TextField();
    private final TableView<PartidoVista> tabla = new TableView<>();
    private final TableView<ClasificacionService.Fila> clasificacion = new TableView<>();
    private final Label estado = new Label("Selecciona una temporada o importa un CSV real.");
    private List<PartidoVista> cargados = new ArrayList<>();

    public static void main(String[] args) { launch(args); }

    @Override public void start(Stage stage) {
        stage.setTitle("LaLiga Stats · Proyecto DAM");
        Label titulo = new Label("Histórico de Primera División española");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        busqueda.setPromptText("Filtrar por equipo");
        busqueda.textProperty().addListener((o,antes,ahora) -> filtrar());

        Button importar = new Button("Importar CSV");
        importar.setOnAction(e -> importar(stage));
        Button actualizar = new Button("Actualizar");
        actualizar.setOnAction(e -> cargarTemporadas());
        temporadas.setPromptText("Temporada");
        temporadas.setOnAction(e -> cargarPartidos());

        HBox barra = new HBox(10, new Label("Temporada:"), temporadas, busqueda, actualizar, importar);
        barra.setStyle("-fx-alignment: center-left;");
        HBox.setHgrow(busqueda, Priority.ALWAYS);

        columna(tabla, "Fecha", p -> p.fecha().toString(), 100);
        columna(tabla, "Local", PartidoVista::local, 150);
        columna(tabla, "Visitante", PartidoVista::visitante, 150);
        columna(tabla, "Resultado", PartidoVista::resultado, 100);
        columna(tabla, "Remates", PartidoVista::remates, 105);
        columna(tabla, "A puerta", PartidoVista::rematesPuerta, 105);
        columna(tabla, "Córners", PartidoVista::corners, 105);
        columna(tabla, "Amarillas", PartidoVista::amarillas, 105);
        columna(tabla, "Rojas", PartidoVista::rojas, 105);
        columna(tabla, "Faltas", PartidoVista::faltas, 105);
        tabla.setPlaceholder(new Label("Importa un CSV de LaLiga o selecciona una temporada."));

        columna(clasificacion, "Equipo", ClasificacionService.Fila::equipo, 170);
        columna(clasificacion, "PJ", f -> ""+f.jugados(), 62);
        columna(clasificacion, "V", f -> ""+f.victorias(), 55);
        columna(clasificacion, "E", f -> ""+f.empates(), 55);
        columna(clasificacion, "D", f -> ""+f.derrotas(), 55);
        columna(clasificacion, "GF", f -> ""+f.golesFavor(), 60);
        columna(clasificacion, "GC", f -> ""+f.golesContra(), 60);
        columna(clasificacion, "DG", f -> ""+f.diferencia(), 60);
        columna(clasificacion, "Pts", f -> ""+f.puntos(), 65);

        VBox panelClasificacion = new VBox(6,
                new Label("Clasificación calculada. Desempate simplificado por diferencia de goles; no equivale al criterio oficial de LaLiga."),
                clasificacion);
        VBox.setVgrow(clasificacion, Priority.ALWAYS);
        Tab partidosTab = new Tab("Partidos", tabla);
        Tab clasificacionTab = new Tab("Clasificación", panelClasificacion);
        partidosTab.setClosable(false);
        clasificacionTab.setClosable(false);
        TabPane pestanias = new TabPane(partidosTab, clasificacionTab);
        VBox.setVgrow(pestanias, Priority.ALWAYS);

        estado.setWrapText(true);
        VBox raiz = new VBox(13, titulo, barra, pestanias, estado);
        raiz.setPadding(new Insets(17));
        Scene scene = new Scene(raiz, 1050, 620);
        stage.setScene(scene);
        stage.show();
        cargarTemporadas();
    }

    private static <T> void columna(TableView<T> vista, String nombre,
                                    Function<T,String> texto, double ancho) {
        TableColumn<T,String> col = new TableColumn<>(nombre);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(texto.apply(d.getValue())));
        col.setPrefWidth(ancho);
        vista.getColumns().add(col);
    }

    private void cargarTemporadas() {
        Integer seleccionAnterior = temporadas.getValue();
        try {
            List<Integer> anios = repo.listarTemporadas();
            temporadas.getItems().setAll(anios);
            if (!anios.isEmpty()) {
                temporadas.setValue(seleccionAnterior != null && anios.contains(seleccionAnterior)
                        ? seleccionAnterior : anios.get(0));
                cargarPartidos();
            } else {
                cargados = List.of();
                filtrar();
                estado.setText("Base de datos vacía. Descarga un CSV de Primera División (SP1) e impórtalo.");
            }
        } catch (SQLException ex) { error("No se puede acceder a MySQL: " + ex.getMessage()); }
    }

    private void cargarPartidos() {
        if (temporadas.getValue() == null) return;
        try {
            cargados = repo.listarPartidos(temporadas.getValue());
            filtrar();
            estado.setText("Temporada " + temporadas.getValue() + "/"
                    + String.format("%02d",(temporadas.getValue()+1)%100)
                    + " · " + cargados.size() + " partidos importados");
        } catch (SQLException ex) { error("Error al consultar MySQL: " + ex.getMessage()); }
    }

    private void filtrar() {
        String filtro = busqueda.getText() == null ? "" : busqueda.getText().trim().toLowerCase();
        List<PartidoVista> visibles = cargados.stream()
                .filter(p -> p.local().toLowerCase().contains(filtro)
                          || p.visitante().toLowerCase().contains(filtro))
                .toList();
        tabla.setItems(FXCollections.observableArrayList(visibles));
        // Clasificación de todos los partidos de temporada, no de la búsqueda por equipo.
        clasificacion.setItems(FXCollections.observableArrayList(clasificador.calcular(cargados)));
    }

    private void importar(Stage stage) {
        FileChooser dialogo = new FileChooser();
        dialogo.setTitle("Selecciona el archivo CSV de Primera División española");
        dialogo.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        File archivo = dialogo.showOpenDialog(stage);
        if (archivo == null) return;

        TextInputDialog prompt = new TextInputDialog("2025");
        prompt.setTitle("Importar temporada");
        prompt.setHeaderText("Indica el primer año de la temporada del CSV");
        prompt.setContentText("Ejemplo: 2025 para 2025/26");
        var valor = prompt.showAndWait();
        if (valor.isEmpty()) return;
        int inicio;
        try {
            inicio = Integer.parseInt(valor.get().trim());
            if (inicio < 2000 || inicio > LocalDate.now().getYear())
                throw new NumberFormatException("Año fuera de rango");
        } catch (NumberFormatException ex) {
            error("Año incorrecto: escribe el año inicial, por ejemplo 2025.");
            return;
        }

        try {
            int cantidad = repo.importar(inicio, parser.leer(archivo.toPath()));
            cargarTemporadas();
            temporadas.setValue(inicio);
            cargarPartidos();
            estado.setText("CSV importado: " + cantidad
                    + " partidos leídos (reimportar actualiza, no duplica).");
        } catch (Exception ex) {
            error("Error al importar CSV: " + ex.getMessage());
        }
    }

    private void error(String mensaje) {
        estado.setText(mensaje);
        Alert aviso = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        aviso.setHeaderText("Operación no completada");
        aviso.showAndWait();
    }
}
