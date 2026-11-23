package es.fraydiegotadeo.laliga;

import es.fraydiegotadeo.laliga.importer.CsvCalendarioParser;
import es.fraydiegotadeo.laliga.importer.CsvPartidosParser;
import es.fraydiegotadeo.laliga.model.PartidoHistorico;
import es.fraydiegotadeo.laliga.model.PartidoPendiente;
import es.fraydiegotadeo.laliga.model.PartidoVista;
import es.fraydiegotadeo.laliga.repository.PartidosRepository;
import es.fraydiegotadeo.laliga.service.BacktestingService;
import es.fraydiegotadeo.laliga.service.ClasificacionService;
import es.fraydiegotadeo.laliga.service.ModeloPoisson;
import es.fraydiegotadeo.laliga.service.Prediccion;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Ventana principal (provisional): importar CSV, consultar partidos y clasificación por temporada
 * y predecir la próxima jornada.
 *
 * Una aplicación JavaFX hereda de Application. JavaFX llama a start() al arrancar y nos da el
 * Stage (la ventana). Dentro del Stage se pone una Scene con los controles organizados en
 * contenedores (VBox = en columna, HBox = en fila).
 *
 * Más adelante esta clase se dividirá en varias pantallas para separar mejor la interfaz de la lógica.
 */
public final class App extends Application {

    /** Primera temporada de LaLiga (1929). Football-Data tiene CSV desde 1993/94. */
    private static final int PRIMER_ANIO_LALIGA = 1929;

    // Objetos de las otras capas que usa la interfaz
    private final PartidosRepository repositorio = new PartidosRepository();
    private final CsvPartidosParser parser = new CsvPartidosParser();
    private final ClasificacionService servicioClasificacion = new ClasificacionService();

    // Controles de la ventana que se usan desde varios métodos
    private final ComboBox<Integer> selectorTemporada = new ComboBox<>();
    private final TextField filtroEquipo = new TextField();
    private final TableView<PartidoVista> tablaPartidos = new TableView<>();
    private final TableView<ClasificacionService.Fila> tablaClasificacion = new TableView<>();
    private final TableView<Prediccion> tablaPrediccion = new TableView<>();
    private final Label infoModelo = new Label("Pulsa el botón para descargar el calendario y calcular la predicción.");
    private final Label estado = new Label("Selecciona una temporada o importa un CSV real.");

    /** Lector del calendario de la próxima jornada y archivo donde se guarda al descargarlo. */
    private final CsvCalendarioParser calendario = new CsvCalendarioParser();
    private static final Path ARCHIVO_CALENDARIO = Path.of("data", "fixtures.csv");

    /** Partidos de la temporada seleccionada, sin filtrar. */
    private List<PartidoVista> partidosCargados = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage ventana) {
        ventana.setTitle("LaLiga Stats · Proyecto DAM");

        Label titulo = new Label("Histórico de Primera División española");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        configurarTablaPartidos();
        configurarTablaClasificacion();
        configurarTablaPrediccion();
        estado.setWrapText(true);

        // Composición de la ventana de arriba a abajo
        VBox raiz = new VBox(13, titulo, crearBarraSuperior(ventana), crearPestanias(), estado);
        raiz.setPadding(new Insets(17));

        ventana.setScene(new Scene(raiz, 1050, 620));
        ventana.show();

        cargarTemporadas();
    }

    /** Barra con el selector de temporada, el filtro y los botones. */
    private HBox crearBarraSuperior(Stage ventana) {
        selectorTemporada.setPromptText("Temporada");
        selectorTemporada.setOnAction(evento -> cargarPartidos());

        filtroEquipo.setPromptText("Filtrar por equipo");
        // Listener: cada vez que cambia el texto del filtro se vuelve a filtrar la tabla
        filtroEquipo.textProperty().addListener((propiedad, textoAnterior, textoNuevo) -> filtrar());

        Button botonActualizar = new Button("Actualizar");
        botonActualizar.setOnAction(evento -> cargarTemporadas());

        Button botonImportar = new Button("Importar CSV");
        botonImportar.setOnAction(evento -> importar(ventana));

        HBox barra = new HBox(10, new Label("Temporada:"), selectorTemporada, filtroEquipo,
                botonActualizar, botonImportar);
        barra.setStyle("-fx-alignment: center-left;");
        // El filtro ocupa todo el espacio libre de la barra
        HBox.setHgrow(filtroEquipo, Priority.ALWAYS);
        return barra;
    }

    private TabPane crearPestanias() {
        Label avisoDesempate = new Label("Clasificación calculada. Desempate simplificado por diferencia "
                + "de goles; no equivale al criterio oficial de LaLiga.");
        VBox panelClasificacion = new VBox(6, avisoDesempate, tablaClasificacion);
        VBox.setVgrow(tablaClasificacion, Priority.ALWAYS);

        Tab pestaniaPartidos = new Tab("Partidos", tablaPartidos);
        Tab pestaniaClasificacion = new Tab("Clasificación", panelClasificacion);
        Tab pestaniaPrediccion = new Tab("Próxima jornada", crearPanelPrediccion());
        pestaniaPartidos.setClosable(false);
        pestaniaClasificacion.setClosable(false);
        pestaniaPrediccion.setClosable(false);

        TabPane pestanias = new TabPane(pestaniaPartidos, pestaniaClasificacion, pestaniaPrediccion);
        VBox.setVgrow(pestanias, Priority.ALWAYS);
        return pestanias;
    }

    private void configurarTablaPartidos() {
        agregarColumna(tablaPartidos, "Fecha", partido -> partido.fecha().toString(), 100);
        agregarColumna(tablaPartidos, "Local", PartidoVista::local, 150);
        agregarColumna(tablaPartidos, "Visitante", PartidoVista::visitante, 150);
        agregarColumna(tablaPartidos, "Resultado", PartidoVista::resultado, 100);
        agregarColumna(tablaPartidos, "Remates", PartidoVista::remates, 105);
        agregarColumna(tablaPartidos, "A puerta", PartidoVista::rematesPuerta, 105);
        agregarColumna(tablaPartidos, "Córners", PartidoVista::corners, 105);
        agregarColumna(tablaPartidos, "Amarillas", PartidoVista::amarillas, 105);
        agregarColumna(tablaPartidos, "Rojas", PartidoVista::rojas, 105);
        agregarColumna(tablaPartidos, "Faltas", PartidoVista::faltas, 105);
        tablaPartidos.setPlaceholder(new Label("Importa un CSV de LaLiga o selecciona una temporada."));
    }

    private void configurarTablaClasificacion() {
        agregarColumna(tablaClasificacion, "Equipo", ClasificacionService.Fila::equipo, 170);
        agregarColumna(tablaClasificacion, "PJ", fila -> String.valueOf(fila.jugados()), 62);
        agregarColumna(tablaClasificacion, "V", fila -> String.valueOf(fila.victorias()), 55);
        agregarColumna(tablaClasificacion, "E", fila -> String.valueOf(fila.empates()), 55);
        agregarColumna(tablaClasificacion, "D", fila -> String.valueOf(fila.derrotas()), 55);
        agregarColumna(tablaClasificacion, "GF", fila -> String.valueOf(fila.golesFavor()), 60);
        agregarColumna(tablaClasificacion, "GC", fila -> String.valueOf(fila.golesContra()), 60);
        agregarColumna(tablaClasificacion, "DG", fila -> String.valueOf(fila.diferencia()), 60);
        agregarColumna(tablaClasificacion, "Pts", fila -> String.valueOf(fila.puntos()), 65);
    }

    /** Panel de la pestaña "Próxima jornada": botón, fiabilidad del modelo, aviso y tabla. */
    private VBox crearPanelPrediccion() {
        Button botonPredecir = new Button("Descargar calendario y predecir");
        botonPredecir.setOnAction(evento -> predecirProximaJornada());

        Label aviso = new Label("Son estimaciones del modelo de Poisson con los partidos de esta temporada "
                + "y la anterior, no resultados seguros. Los valores con decimales son medias esperadas.");
        aviso.setWrapText(true);
        infoModelo.setWrapText(true);

        VBox panel = new VBox(8, botonPredecir, infoModelo, aviso, tablaPrediccion);
        panel.setPadding(new Insets(8, 0, 0, 0));
        VBox.setVgrow(tablaPrediccion, Priority.ALWAYS);
        return panel;
    }

    private void configurarTablaPrediccion() {
        agregarColumna(tablaPrediccion, "Fecha", prediccion -> prediccion.fecha().toString(), 90);
        agregarColumna(tablaPrediccion, "Local", Prediccion::local, 115);
        agregarColumna(tablaPrediccion, "Visitante", Prediccion::visitante, 115);
        agregarColumna(tablaPrediccion, "1", prediccion -> Prediccion.porcentaje(prediccion.probLocal()), 52);
        agregarColumna(tablaPrediccion, "X", prediccion -> Prediccion.porcentaje(prediccion.probEmpate()), 52);
        agregarColumna(tablaPrediccion, "2", prediccion -> Prediccion.porcentaje(prediccion.probVisitante()), 52);
        agregarColumna(tablaPrediccion, "Pronóstico", Prediccion::signoMasProbable, 80);
        agregarColumna(tablaPrediccion, "Goles esp.", Prediccion::goles, 82);
        agregarColumna(tablaPrediccion, "Marcador", Prediccion::resultadoMasProbable, 75);
        agregarColumna(tablaPrediccion, "+2,5 goles", prediccion -> Prediccion.porcentaje(prediccion.probMasDe2()), 78);
        agregarColumna(tablaPrediccion, "Córners", Prediccion::corners, 82);
        agregarColumna(tablaPrediccion, "Amarillas", Prediccion::amarillas, 82);
        agregarColumna(tablaPrediccion, "Remates", Prediccion::remates, 85);
        agregarColumna(tablaPrediccion, "Faltas", Prediccion::faltas, 85);
        tablaPrediccion.setPlaceholder(new Label("Sin predicciones todavía."));
    }

    /**
     * Descarga el calendario, predice cada partido de LaLiga que aparece y muestra
     * lo que acertó el modelo en la temporada anterior completa.
     */
    private void predecirProximaJornada() {
        try {
            String origen = "descargado de Football-Data";
            try {
                calendario.descargar(ARCHIVO_CALENDARIO);
            } catch (IOException ex) {
                // Sin internet se usa el último calendario descargado, si existe
                if (!Files.exists(ARCHIVO_CALENDARIO)) {
                    throw ex;
                }
                origen = "sin conexión, se usa el último descargado";
            }
            List<PartidoPendiente> pendientes = calendario.leer(ARCHIVO_CALENDARIO);
            if (pendientes.isEmpty()) {
                mostrarError("El calendario no tiene partidos de LaLiga ahora mismo.");
                return;
            }

            List<Integer> temporadas = repositorio.listarTemporadas();
            if (temporadas.isEmpty()) {
                mostrarError("Importa antes las temporadas desde CSV.");
                return;
            }
            // listarTemporadas devuelve primero la más reciente
            int actual = temporadas.get(0);
            List<PartidoVista> historial = new ArrayList<>(repositorio.listarPartidos(actual - 1));
            historial.addAll(repositorio.listarPartidos(actual));

            List<Prediccion> predicciones = new ArrayList<>();
            for (PartidoPendiente partido : pendientes) {
                // Un modelo por partido, entrenado solo con lo jugado antes de su fecha
                ModeloPoisson modelo = new ModeloPoisson(historial, partido.fecha());
                predicciones.add(modelo.predecir(partido.fecha(), partido.local(), partido.visitante()));
            }
            tablaPrediccion.setItems(FXCollections.observableArrayList(predicciones));

            mostrarFiabilidad(actual - 1);
            estado.setText(predicciones.size() + " partidos predichos (calendario " + origen + ").");
        } catch (IOException | SQLException | IllegalStateException ex) {
            mostrarError("No se ha podido hacer la predicción: " + ex.getMessage());
        }
    }

    /** Hace el backtesting de una temporada ya terminada y lo escribe encima de la tabla. */
    private void mostrarFiabilidad(int temporada) throws SQLException {
        List<PartidoVista> historial = new ArrayList<>(repositorio.listarPartidos(temporada - 1));
        List<PartidoVista> aEvaluar = repositorio.listarPartidos(temporada);
        historial.addAll(aEvaluar);

        BacktestingService.Resultado resultado = new BacktestingService().evaluar(historial, aEvaluar);
        if (resultado == null) {
            infoModelo.setText("No hay datos suficientes para medir la fiabilidad del modelo.");
            return;
        }
        infoModelo.setText("Fiabilidad: prediciendo los " + resultado.partidos() + " partidos de la temporada "
                + temporada + "/" + String.format("%02d", (temporada + 1) % 100)
                + " sin mirar el futuro, el modelo acertó el " + Prediccion.porcentaje(resultado.porcentajeAciertos())
                + " de los 1-X-2 (elegir siempre el resultado más frecuente: "
                + Prediccion.porcentaje(resultado.porcentajeAciertosBase()) + ").");
    }

    /**
     * Añade una columna de texto a una tabla. Es genérico (<T>) para servir tanto para la tabla
     * de partidos como para la de clasificación.
     *
     * @param tabla  tabla a la que se añade la columna
     * @param nombre título de la columna
     * @param texto  función que, dado un objeto de la fila, devuelve el texto a mostrar
     * @param ancho  ancho preferido en píxeles
     */
    private static <T> void agregarColumna(TableView<T> tabla, String nombre,
                                           Function<T, String> texto, double ancho) {
        TableColumn<T, String> columna = new TableColumn<>(nombre);
        // Para cada celda, JavaFX nos da el objeto de esa fila y le aplicamos la función
        columna.setCellValueFactory(celda -> new ReadOnlyStringWrapper(texto.apply(celda.getValue())));
        columna.setPrefWidth(ancho);
        tabla.getColumns().add(columna);
    }

    /** Rellena el selector con las temporadas guardadas en MySQL y carga la seleccionada. */
    private void cargarTemporadas() {
        Integer seleccionAnterior = selectorTemporada.getValue();
        try {
            List<Integer> anios = repositorio.listarTemporadas();
            selectorTemporada.getItems().setAll(anios);

            if (anios.isEmpty()) {
                partidosCargados = List.of();
                filtrar();
                estado.setText("Base de datos vacía. Descarga un CSV de Primera División (SP1) e impórtalo.");
                return;
            }

            // Mantener la temporada que estaba elegida; si ya no existe, elegir la más reciente
            if (seleccionAnterior != null && anios.contains(seleccionAnterior)) {
                selectorTemporada.setValue(seleccionAnterior);
            } else {
                selectorTemporada.setValue(anios.get(0));
            }
            cargarPartidos();
        } catch (SQLException ex) {
            mostrarError("No se puede acceder a MySQL: " + ex.getMessage());
        }
    }

    /** Consulta en MySQL los partidos de la temporada seleccionada. */
    private void cargarPartidos() {
        Integer temporada = selectorTemporada.getValue();
        if (temporada == null) {
            return;
        }
        try {
            partidosCargados = repositorio.listarPartidos(temporada);
            filtrar();
            estado.setText("Temporada " + temporada + "/" + String.format("%02d", (temporada + 1) % 100)
                    + " · " + partidosCargados.size() + " partidos importados");
        } catch (SQLException ex) {
            mostrarError("Error al consultar MySQL: " + ex.getMessage());
        }
    }

    /** Muestra en la tabla solo los partidos del equipo escrito en el filtro y recalcula la clasificación. */
    private void filtrar() {
        String filtro = filtroEquipo.getText() == null ? "" : filtroEquipo.getText().trim().toLowerCase();

        List<PartidoVista> visibles = new ArrayList<>();
        for (PartidoVista partido : partidosCargados) {
            if (partido.local().toLowerCase().contains(filtro)
                    || partido.visitante().toLowerCase().contains(filtro)) {
                visibles.add(partido);
            }
        }
        tablaPartidos.setItems(FXCollections.observableArrayList(visibles));

        // La clasificación usa siempre todos los partidos de la temporada, no solo los filtrados
        tablaClasificacion.setItems(FXCollections.observableArrayList(
                servicioClasificacion.calcular(partidosCargados)));
    }

    /** Pide un CSV y el año de la temporada, lo lee y lo guarda en MySQL. */
    private void importar(Stage ventana) {
        FileChooser dialogoArchivo = new FileChooser();
        dialogoArchivo.setTitle("Selecciona el archivo CSV de Primera División española");
        dialogoArchivo.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        File archivo = dialogoArchivo.showOpenDialog(ventana);
        if (archivo == null) {
            return; // El usuario ha cancelado
        }

        Integer inicio = pedirAnioTemporada();
        if (inicio == null) {
            return;
        }

        try {
            List<PartidoHistorico> partidos = parser.leer(archivo.toPath());
            int cantidad = repositorio.importar(inicio, partidos);
            cargarTemporadas();
            selectorTemporada.setValue(inicio);
            cargarPartidos();
            estado.setText("CSV importado: " + cantidad + " partidos leídos (reimportar actualiza, no duplica).");
        } catch (Exception ex) {
            mostrarError("Error al importar CSV: " + ex.getMessage());
        }
    }

    /**
     * Pregunta el año de inicio de la temporada del CSV.
     *
     * @return el año, o null si el usuario cancela o escribe un valor no válido
     */
    private Integer pedirAnioTemporada() {
        TextInputDialog dialogo = new TextInputDialog("2025");
        dialogo.setTitle("Importar temporada");
        dialogo.setHeaderText("Indica el primer año de la temporada del CSV");
        dialogo.setContentText("Ejemplo: 2025 para 2025/26");

        Optional<String> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty()) {
            return null;
        }
        try {
            int anio = Integer.parseInt(respuesta.get().trim());
            // LaLiga empezó en 1929, así que no tiene sentido un año anterior ni uno del futuro
            if (anio < PRIMER_ANIO_LALIGA || anio > LocalDate.now().getYear()) {
                throw new NumberFormatException("Año fuera de rango");
            }
            return anio;
        } catch (NumberFormatException ex) {
            mostrarError("Año incorrecto: escribe el año inicial, por ejemplo 2025.");
            return null;
        }
    }

    /** Muestra el error en la barra de estado y en una ventana de aviso. */
    private void mostrarError(String mensaje) {
        estado.setText(mensaje);
        Alert aviso = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        aviso.setHeaderText("Operación no completada");
        aviso.showAndWait();
    }
}
