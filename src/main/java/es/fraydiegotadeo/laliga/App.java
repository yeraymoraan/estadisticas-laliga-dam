package es.fraydiegotadeo.laliga;

import es.fraydiegotadeo.laliga.importer.ClienteFootballData;
import es.fraydiegotadeo.laliga.model.PartidoVista;
import es.fraydiegotadeo.laliga.repository.PartidosRepository;
import es.fraydiegotadeo.laliga.service.ActualizacionService;
import es.fraydiegotadeo.laliga.service.ClasificacionService;
import es.fraydiegotadeo.laliga.service.PrediccionService;
import es.fraydiegotadeo.laliga.service.SimulacionService;
import es.fraydiegotadeo.laliga.ui.VistaClasificacion;
import es.fraydiegotadeo.laliga.ui.VistaPartidos;
import es.fraydiegotadeo.laliga.ui.VistaPrediccion;
import es.fraydiegotadeo.laliga.ui.VistaSimulacion;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

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
    private final SimulacionService servicioSimulacion = new SimulacionService();

    /** Temporadas que se simulan: con 10.000 las probabilidades apenas cambian de una vez a otra. */
    private static final int SIMULACIONES = 10_000;
    private static final long SEMILLA = 2026;

    // Pantallas
    private final VistaPartidos vistaPartidos = new VistaPartidos();
    private final VistaClasificacion vistaClasificacion = new VistaClasificacion();
    private final VistaPrediccion vistaPrediccion = new VistaPrediccion();
    private final VistaSimulacion vistaSimulacion = new VistaSimulacion();

    // Menú lateral
    private final ToggleButton botonJornada = new ToggleButton("Próxima jornada");
    private final ToggleButton botonSimulacion = new ToggleButton("Simulación");
    private final ToggleButton botonClasificacion = new ToggleButton("Clasificación");
    private final ToggleButton botonPartidos = new ToggleButton("Partidos");
    /** Es un campo para poder desactivarlo mientras descarga. */
    private final Button botonActualizar = new Button("Actualizar datos");
    private final Label estado = new Label("Cargando datos...");

    // Cabecera y zona donde se pone la pantalla activa
    private final Label tituloPantalla = new Label();
    private final Label subtituloPantalla = new Label();
    private final ComboBox<Integer> selectorTemporada = new ComboBox<>();
    /** StackPane: contenedor que muestra sus hijos uno encima de otro; aquí solo tiene la pantalla activa. */
    private final StackPane zonaPantalla = new StackPane();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage ventana) {
        ventana.setTitle("LaLiga Stats · Proyecto DAM");

        // BorderPane divide la ventana en zonas: el menú a la izquierda (left) y la pantalla en el centro
        BorderPane raiz = new BorderPane();
        raiz.setLeft(crearMenu());
        raiz.setCenter(crearContenido());

        Scene escena = new Scene(raiz, 1280, 800);
        // La hoja de estilos está en src/main/resources; getResource la busca dentro del programa
        escena.getStylesheets().add(getClass().getResource("/es/fraydiegotadeo/laliga/ui/estilos.css").toExternalForm());
        ventana.setScene(escena);
        ventana.setMinWidth(1000);
        ventana.setMinHeight(650);
        ventana.show();

        // Al abrir se enseña la próxima jornada, que es la pantalla principal
        botonJornada.setSelected(true);
        mostrarJornada();

        // Primero se enseña lo que ya hay en MySQL y después se buscan datos nuevos en internet
        cargarTemporadas();
        actualizarDatos();
    }

    /** Menú lateral oscuro: logo, botones de las pantallas y, abajo, el estado de los datos. */
    private VBox crearMenu() {
        Label logo = new Label("LaLiga");
        logo.getStyleClass().add("logo");
        Label logoAcento = new Label("Stats");
        logoAcento.getStyleClass().add("logo-acento");
        HBox titulo = new HBox(5, logo, logoAcento);
        Label subtitulo = new Label("Estadísticas y predicciones");
        subtitulo.getStyleClass().add("logo-sub");

        // ToggleGroup: de los botones del grupo solo puede haber uno pulsado a la vez
        ToggleGroup grupo = new ToggleGroup();
        prepararBotonMenu(botonJornada, "nav-jornada", grupo);
        prepararBotonMenu(botonSimulacion, "nav-simulacion", grupo);
        prepararBotonMenu(botonClasificacion, "nav-clasificacion", grupo);
        prepararBotonMenu(botonPartidos, "nav-partidos", grupo);
        botonJornada.setOnAction(evento -> mostrarJornada());
        botonSimulacion.setOnAction(evento -> mostrarSimulacion());
        botonClasificacion.setOnAction(evento -> mostrarClasificacion());
        botonPartidos.setOnAction(evento -> mostrarPartidos());
        // Si se pulsa otra vez el botón activo se desmarcaría; así se evita que no quede ninguno
        grupo.selectedToggleProperty().addListener((propiedad, anterior, nuevo) -> {
            if (nuevo == null) {
                anterior.setSelected(true);
            }
        });

        // Region vacía que crece y empuja el botón de actualizar hasta abajo del menú
        Region hueco = new Region();
        VBox.setVgrow(hueco, Priority.ALWAYS);

        botonActualizar.getStyleClass().add("boton-menu");
        botonActualizar.setOnAction(evento -> actualizarDatos());
        estado.getStyleClass().add("estado-menu");
        estado.setWrapText(true);

        VBox menu = new VBox(titulo, subtitulo, botonJornada, botonSimulacion, botonClasificacion, botonPartidos,
                hueco, botonActualizar, estado);
        menu.getStyleClass().add("menu");
        return menu;
    }

    /** Da a un botón del menú su id (para el CSS), su estilo y su grupo. */
    private static void prepararBotonMenu(ToggleButton boton, String id, ToggleGroup grupo) {
        boton.setId(id);
        boton.getStyleClass().add("nav-boton");
        boton.setToggleGroup(grupo);
    }

    /** Zona central: cabecera (título, subtítulo y selector de temporada) y la pantalla activa. */
    private VBox crearContenido() {
        tituloPantalla.getStyleClass().add("titulo-pantalla");
        subtituloPantalla.getStyleClass().add("subtitulo-pantalla");
        VBox textos = new VBox(2, tituloPantalla, subtituloPantalla);

        selectorTemporada.setPromptText("Temporada");
        // El selector guarda el año de inicio (2026) pero enseña "Temporada 2026/27"
        selectorTemporada.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer inicio) {
                if (inicio == null) {
                    return "";
                }
                return "Temporada " + inicio + "/" + String.format("%02d", (inicio + 1) % 100);
            }

            @Override
            public Integer fromString(String texto) {
                return null; // No se usa: el selector no deja escribir
            }
        });
        selectorTemporada.setOnAction(evento -> cargarPartidos());

        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        HBox cabecera = new HBox(textos, hueco, selectorTemporada);
        cabecera.setStyle("-fx-alignment: center-left;");

        VBox contenido = new VBox(cabecera, zonaPantalla);
        contenido.getStyleClass().add("contenido");
        VBox.setVgrow(zonaPantalla, Priority.ALWAYS);
        return contenido;
    }

    /** Cambia la pantalla del centro. El selector de temporada solo se ve donde sirve. */
    private void cambiarPantalla(String titulo, String subtitulo, Node pantalla, boolean conTemporada) {
        tituloPantalla.setText(titulo);
        subtituloPantalla.setText(subtitulo);
        selectorTemporada.setVisible(conTemporada);
        zonaPantalla.getChildren().setAll(pantalla);
    }

    private void mostrarJornada() {
        cambiarPantalla("Próxima jornada", "Predicción de cada partido con el modelo de Poisson",
                vistaPrediccion.getRaiz(), false);
    }

    private void mostrarSimulacion() {
        cambiarPantalla("Simulación de la liga", "Cómo puede terminar la temporada en curso (método de Monte Carlo)",
                vistaSimulacion.getRaiz(), false);
    }

    private void mostrarClasificacion() {
        cambiarPantalla("Clasificación", "Tabla calculada con los resultados guardados",
                vistaClasificacion.getRaiz(), true);
    }

    private void mostrarPartidos() {
        cambiarPantalla("Partidos", "Resultados y estadísticas de cada partido", vistaPartidos.getRaiz(), true);
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
            predecirProximaJornada();
            simularTemporada();
        });
        tarea.setOnFailed(evento -> {
            botonActualizar.setDisable(false);
            mostrarError("No se han podido actualizar los datos: " + tarea.getException().getMessage());
            // Aunque falle la actualización se intenta predecir con los datos guardados
            predecirProximaJornada();
            simularTemporada();
        });

        // setDaemon(true): si se cierra la ventana, este hilo no impide que el programa termine
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Texto corto del estado de los datos, que se ve abajo del menú. */
    private static String textoResumen(ActualizacionService.Resumen resumen) {
        String texto;
        if (resumen.ultimoPartido() == null) {
            texto = "Datos actualizados.";
        } else {
            texto = "Datos al día · último partido "
                    + resumen.ultimoPartido().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        for (String aviso : resumen.avisos()) {
            texto += "\n" + aviso;
        }
        return texto;
    }

    /**
     * Predice la próxima jornada en segundo plano y la muestra en su pantalla.
     * Se llama sola después de actualizar los datos, así el usuario no tiene que pulsar nada.
     */
    private void predecirProximaJornada() {
        vistaPrediccion.mostrarMensaje("Calculando predicciones...");

        Task<PrediccionService.Jornada> tarea = new Task<>() {
            @Override
            protected PrediccionService.Jornada call() throws Exception {
                return servicioPrediccion.predecirProximaJornada();
            }
        };
        tarea.setOnSucceeded(evento -> vistaPrediccion.mostrar(tarea.getValue()));
        tarea.setOnFailed(evento -> vistaPrediccion.mostrarMensaje(
                "No se ha podido calcular la predicción: " + tarea.getException().getMessage()));

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Simula en segundo plano el final de la temporada en curso y lo muestra en su pantalla.
     * Se usa una semilla fija para que, con los mismos datos, salga siempre el mismo resultado.
     */
    private void simularTemporada() {
        vistaSimulacion.mostrarMensaje("Simulando la temporada...");

        Task<SimulacionService.Resultado> tarea = new Task<>() {
            @Override
            protected SimulacionService.Resultado call() throws Exception {
                List<Integer> temporadas = repositorio.listarTemporadas();
                if (temporadas.isEmpty()) {
                    throw new IllegalStateException("todavía no hay partidos guardados");
                }
                int actual = temporadas.get(0);
                // Fecha de corte = mañana: cuenta como jugado todo lo que ya tiene resultado
                return servicioSimulacion.simular(repositorio.listarPartidos(actual),
                        repositorio.listarPartidos(actual - 1), LocalDate.now().plusDays(1),
                        SIMULACIONES, SEMILLA);
            }
        };
        tarea.setOnSucceeded(evento -> vistaSimulacion.mostrar(tarea.getValue()));
        tarea.setOnFailed(evento -> vistaSimulacion.mostrarMensaje(
                "No se ha podido simular la temporada: " + tarea.getException().getMessage()));

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
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
