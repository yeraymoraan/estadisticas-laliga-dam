package es.fraydiegotadeo.laliga;

import es.fraydiegotadeo.laliga.importer.ClienteFootballData;
import es.fraydiegotadeo.laliga.model.PartidoVista;
import es.fraydiegotadeo.laliga.repository.PartidosRepository;
import es.fraydiegotadeo.laliga.service.ActualizacionService;
import es.fraydiegotadeo.laliga.service.ClasificacionService;
import es.fraydiegotadeo.laliga.service.PrediccionService;
import es.fraydiegotadeo.laliga.ui.VistaClasificacion;
import es.fraydiegotadeo.laliga.ui.VistaPartidos;
import es.fraydiegotadeo.laliga.ui.VistaPrediccion;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Ventana principal. Une las pantallas del paquete ui y les pasa los datos de la capa service.
 * Los datos se descargan solos de Football-Data (ActualizacionService); el usuario no importa nada.
 *
 * Una aplicación JavaFX hereda de Application. JavaFX llama a start() al arrancar y nos da el
 * Stage (la ventana). Dentro del Stage se pone una Scene con los controles organizados en
 * contenedores (VBox = en columna, HBox = en fila).
 */
public final class App extends Application {

    // Objetos de las otras capas que usa la interfaz
    private final PartidosRepository repositorio = new PartidosRepository();
    private final ClienteFootballData descargas = new ClienteFootballData();
    private final ClasificacionService servicioClasificacion = new ClasificacionService();
    private final ActualizacionService servicioActualizacion = new ActualizacionService(repositorio, descargas);
    private final PrediccionService servicioPrediccion = new PrediccionService(repositorio, descargas);

    // Pantallas
    private final VistaPartidos vistaPartidos = new VistaPartidos();
    private final VistaClasificacion vistaClasificacion = new VistaClasificacion();
    private final VistaPrediccion vistaPrediccion = new VistaPrediccion();

    // Controles comunes a varias pantallas
    private final ComboBox<Integer> selectorTemporada = new ComboBox<>();
    /** Es un campo para poder desactivarlo mientras descarga. */
    private final Button botonActualizar = new Button("Actualizar datos");
    private final Label estado = new Label("Cargando datos...");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage ventana) {
        ventana.setTitle("LaLiga Stats · Proyecto DAM");

        Label titulo = new Label("Histórico de Primera División española");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        estado.setWrapText(true);
        vistaPrediccion.getBotonPredecir().setOnAction(evento -> predecirProximaJornada());

        // Composición de la ventana de arriba a abajo
        VBox raiz = new VBox(13, titulo, crearBarraSuperior(), crearPestanias(), estado);
        raiz.setPadding(new Insets(17));

        ventana.setScene(new Scene(raiz, 1050, 620));
        ventana.show();

        // Primero se enseña lo que ya hay en MySQL y después se buscan datos nuevos en internet
        cargarTemporadas();
        actualizarDatos();
    }

    /** Barra con el selector de temporada y el botón de actualizar. */
    private HBox crearBarraSuperior() {
        selectorTemporada.setPromptText("Temporada");
        selectorTemporada.setOnAction(evento -> cargarPartidos());
        botonActualizar.setOnAction(evento -> actualizarDatos());

        HBox barra = new HBox(10, new Label("Temporada:"), selectorTemporada, botonActualizar);
        barra.setStyle("-fx-alignment: center-left;");
        return barra;
    }

    private TabPane crearPestanias() {
        Tab pestaniaPartidos = new Tab("Partidos", vistaPartidos.getRaiz());
        Tab pestaniaClasificacion = new Tab("Clasificación", vistaClasificacion.getRaiz());
        Tab pestaniaPrediccion = new Tab("Próxima jornada", vistaPrediccion.getRaiz());
        pestaniaPartidos.setClosable(false);
        pestaniaClasificacion.setClosable(false);
        pestaniaPrediccion.setClosable(false);

        TabPane pestanias = new TabPane(pestaniaPartidos, pestaniaClasificacion, pestaniaPrediccion);
        VBox.setVgrow(pestanias, Priority.ALWAYS);
        return pestanias;
    }

    /**
     * Descarga de Football-Data los partidos nuevos y los guarda en MySQL, sin que el usuario importe nada.
     *
     * Se hace en otro hilo con un Task de JavaFX. Si se hiciera en el hilo de la ventana, esta se
     * quedaría congelada mientras descarga (la primera vez son 34 temporadas). JavaFX obliga a que
     * los controles solo se toquen desde su propio hilo, por eso la tabla se recarga en
     * setOnSucceeded, que JavaFX ejecuta en el hilo de la ventana cuando el Task termina.
     */
    private void actualizarDatos() {
        botonActualizar.setDisable(true);
        estado.setText("Buscando partidos nuevos en Football-Data...");

        Task<ActualizacionService.Resumen> tarea = new Task<>() {
            @Override
            protected ActualizacionService.Resumen call() throws Exception {
                return servicioActualizacion.actualizar(LocalDate.now());
            }
        };
        tarea.setOnSucceeded(evento -> {
            botonActualizar.setDisable(false);
            cargarTemporadas();
            estado.setText(textoResumen(tarea.getValue()));
        });
        tarea.setOnFailed(evento -> {
            botonActualizar.setDisable(false);
            mostrarError("No se han podido actualizar los datos: " + tarea.getException().getMessage());
        });

        // setDaemon(true): si se cierra la ventana, este hilo no impide que el programa termine
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Texto de la barra de estado después de actualizar. */
    private static String textoResumen(ActualizacionService.Resumen resumen) {
        String texto;
        if (resumen.ultimoPartido() == null) {
            texto = "Datos actualizados (" + resumen.temporadasActualizadas() + " temporadas).";
        } else {
            texto = "Datos actualizados: último partido guardado el "
                    + resumen.ultimoPartido().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " (Football-Data publica los resultados con unos días de retraso).";
        }
        for (String aviso : resumen.avisos()) {
            texto += " " + aviso;
        }
        return texto;
    }

    /** Predice la próxima jornada y la muestra en su pantalla. */
    private void predecirProximaJornada() {
        try {
            PrediccionService.Jornada jornada = servicioPrediccion.predecirProximaJornada();
            vistaPrediccion.mostrar(jornada);
            String origen = jornada.sinConexion() ? "sin conexión, se usa el último descargado" : "descargado de Football-Data";
            estado.setText(jornada.predicciones().size() + " partidos predichos (calendario " + origen + ").");
        } catch (IOException | SQLException | IllegalStateException ex) {
            mostrarError("No se ha podido hacer la predicción: " + ex.getMessage());
        }
    }

    /** Rellena el selector con las temporadas guardadas en MySQL y carga la seleccionada. */
    private void cargarTemporadas() {
        Integer seleccionAnterior = selectorTemporada.getValue();
        try {
            List<Integer> anios = repositorio.listarTemporadas();
            selectorTemporada.getItems().setAll(anios);

            if (anios.isEmpty()) {
                estado.setText("Todavía no hay partidos guardados: se están descargando de Football-Data.");
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

    /** Consulta en MySQL los partidos de la temporada seleccionada y calcula su clasificación. */
    private void cargarPartidos() {
        Integer temporada = selectorTemporada.getValue();
        if (temporada == null) {
            return;
        }
        try {
            List<PartidoVista> partidos = repositorio.listarPartidos(temporada);
            vistaPartidos.mostrar(partidos);
            vistaClasificacion.mostrar(servicioClasificacion.calcular(partidos));
            estado.setText("Temporada " + temporada + "/" + String.format("%02d", (temporada + 1) % 100)
                    + " · " + partidos.size() + " partidos");
        } catch (SQLException ex) {
            mostrarError("Error al consultar MySQL: " + ex.getMessage());
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
