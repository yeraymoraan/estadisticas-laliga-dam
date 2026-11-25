package es.fraydiegotadeo.laliga.ui;

import es.fraydiegotadeo.laliga.model.NombresEquipos;
import es.fraydiegotadeo.laliga.service.Prediccion;
import es.fraydiegotadeo.laliga.service.PrediccionService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Pantalla "Próxima jornada": una tarjeta por partido con la barra de probabilidades 1-X-2,
 * el marcador más probable y las estadísticas esperadas.
 */
public final class VistaPrediccion {

    /** Ancho de cada tarjeta: caben dos por fila en la ventana normal. */
    private static final double ANCHO_TARJETA = 470;
    /** Ancho de la barra 1-X-2 dentro de la tarjeta. */
    private static final double ANCHO_BARRA = 430;

    private static final Locale ESPANIA = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final Label numeroPartidos = new Label("–");
    private final Label fechasJornada = new Label("Calculando predicciones...");
    /** FlowPane coloca las tarjetas en fila y salta a la siguiente línea cuando no caben. */
    private final FlowPane tarjetas = new FlowPane(16, 16);
    private final VBox raiz;

    public VistaPrediccion() {
        tarjetas.setPrefWrapLength(ANCHO_TARJETA * 2 + 16);

        // ScrollPane: si hay más tarjetas de las que caben, aparece una barra para bajar
        ScrollPane desplazamiento = new ScrollPane(tarjetas);
        desplazamiento.setFitToWidth(true);
        desplazamiento.getStyleClass().add("sin-fondo");

        Label aviso = new Label("Estimaciones estadísticas (modelo de Poisson), no resultados seguros. "
                + "Los valores con decimales son medias esperadas.");
        aviso.getStyleClass().add("texto-suave");

        raiz = new VBox(16, crearResumen(), desplazamiento, aviso);
        VBox.setVgrow(desplazamiento, Priority.ALWAYS);
    }

    public Node getRaiz() {
        return raiz;
    }

    /** Muestra un mensaje mientras se calcula o si algo ha fallado. */
    public void mostrarMensaje(String mensaje) {
        fechasJornada.setText(mensaje);
    }

    /** Pinta una tarjeta por partido y el resumen de la jornada. */
    public void mostrar(PrediccionService.Jornada jornada) {
        List<PrediccionService.PartidoPredicho> partidos = jornada.partidos();
        tarjetas.getChildren().clear();
        int jugados = 0;
        int aciertos = 0;
        for (PrediccionService.PartidoPredicho partido : partidos) {
            tarjetas.getChildren().add(crearTarjeta(partido));
            if (partido.jugado()) {
                jugados++;
            }
            if (partido.acertado()) {
                aciertos++;
            }
        }

        numeroPartidos.setText(String.valueOf(partidos.size()));
        if (partidos.isEmpty()) {
            fechasJornada.setText("El calendario no tiene partidos de LaLiga ahora mismo.");
            return;
        }
        // El calendario viene ordenado por fecha: el primero y el último marcan el rango
        LocalDate primero = partidos.get(0).prediccion().fecha();
        LocalDate ultimo = partidos.get(partidos.size() - 1).prediccion().fecha();
        String texto = "Del " + primero.format(DIA_MES) + " al " + ultimo.format(DIA_MES);
        if (jugados == partidos.size()) {
            texto += " · ya jugada: el modelo acertó " + aciertos + " de " + jugados
                    + ". Football-Data todavía no ha publicado la jornada siguiente.";
        } else if (jugados > 0) {
            texto += " · " + jugados + " ya jugados (" + aciertos + " acertados)";
        }
        if (jornada.sinConexion()) {
            texto += " · sin conexión, calendario guardado";
        }
        fechasJornada.setText(texto);
    }

    /** Tarjeta de resumen de arriba: cuántos partidos tiene la jornada y entre qué fechas. */
    private Node crearResumen() {
        numeroPartidos.getStyleClass().add("resumen-numero");
        Label titulo = new Label("PARTIDOS DE LA JORNADA");
        titulo.getStyleClass().add("resumen-titulo");
        fechasJornada.getStyleClass().add("texto-suave");
        fechasJornada.setWrapText(true);

        VBox resumen = new VBox(2, titulo, numeroPartidos, fechasJornada);
        resumen.getStyleClass().add("tarjeta");
        resumen.setMaxWidth(ANCHO_TARJETA * 2 + 16);
        return resumen;
    }

    /** Tarjeta de un partido. Si ya se ha jugado, se añade el resultado real y si se acertó. */
    private Node crearTarjeta(PrediccionService.PartidoPredicho partido) {
        Prediccion prediccion = partido.prediccion();
        Label fecha = new Label(textoFecha(prediccion.fecha()));
        fecha.getStyleClass().add("tarjeta-fecha");
        Region huecoCabecera = new Region();
        HBox.setHgrow(huecoCabecera, Priority.ALWAYS);
        HBox cabecera = new HBox(fecha, huecoCabecera);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        if (partido.jugado()) {
            cabecera.getChildren().add(crearEtiquetaResultado(partido));
        }

        Label local = new Label(NombresEquipos.completo(prediccion.local()));
        Label contra = new Label("vs");
        Label visitante = new Label(NombresEquipos.completo(prediccion.visitante()));
        local.getStyleClass().add("tarjeta-equipo");
        visitante.getStyleClass().add("tarjeta-equipo");
        contra.getStyleClass().add("texto-suave");
        HBox equipos = new HBox(8, local, contra, visitante);
        equipos.setAlignment(Pos.CENTER_LEFT);

        VBox tarjeta = new VBox(10, cabecera, equipos, crearBarra(prediccion), crearPorcentajes(prediccion),
                crearDatos(prediccion));
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setPrefWidth(ANCHO_TARJETA);
        return tarjeta;
    }

    /** Etiqueta "Final 0-1 · Fallado" (roja) o "Final 2-1 · Acertado" (verde). */
    private static Label crearEtiquetaResultado(PrediccionService.PartidoPredicho partido) {
        String texto = "Final " + partido.resultadoReal().resultado();
        Label etiqueta;
        if (partido.acertado()) {
            etiqueta = new Label(texto + "  ·  Acertado");
            etiqueta.getStyleClass().add("chip-acierto");
        } else {
            etiqueta = new Label(texto + "  ·  Fallado");
            etiqueta.getStyleClass().add("chip-fallo");
        }
        return etiqueta;
    }

    /**
     * Barra dividida en tres trozos (1, X y 2) cuyo ancho es proporcional a su probabilidad.
     * Cada trozo es una Region vacía con un color de fondo puesto por CSS.
     */
    private static Node crearBarra(Prediccion prediccion) {
        Region uno = trozoBarra("barra-1", prediccion.probLocal());
        Region equis = trozoBarra("barra-x", prediccion.probEmpate());
        Region dos = trozoBarra("barra-2", prediccion.probVisitante());
        HBox barra = new HBox(3, uno, equis, dos);
        barra.getStyleClass().add("barra");
        return barra;
    }

    private static Region trozoBarra(String claseColor, double probabilidad) {
        Region trozo = new Region();
        trozo.getStyleClass().addAll("barra-trozo", claseColor);
        double ancho = ANCHO_BARRA * probabilidad;
        trozo.setMinWidth(ancho);
        trozo.setPrefWidth(ancho);
        return trozo;
    }

    /** Fila "1 · 76 %   X · 15 %   2 · 9 %", con el más probable resaltado. */
    private static Node crearPorcentajes(Prediccion prediccion) {
        String signo = prediccion.signoMasProbable();
        Region hueco1 = new Region();
        Region hueco2 = new Region();
        HBox.setHgrow(hueco1, Priority.ALWAYS);
        HBox.setHgrow(hueco2, Priority.ALWAYS);
        return new HBox(
                porcentaje("1  Local", prediccion.probLocal(), signo.equals("1")), hueco1,
                porcentaje("X  Empate", prediccion.probEmpate(), signo.equals("X")), hueco2,
                porcentaje("2  Visitante", prediccion.probVisitante(), signo.equals("2")));
    }

    private static Label porcentaje(String texto, double probabilidad, boolean esElMasProbable) {
        Label etiqueta = new Label(texto + "  " + Prediccion.porcentaje(probabilidad));
        etiqueta.getStyleClass().add(esElMasProbable ? "prob-max" : "prob");
        return etiqueta;
    }

    /**
     * Rejilla con el resto de datos esperados. GridPane coloca los nodos en filas y columnas:
     * add(nodo, columna, fila).
     */
    private static Node crearDatos(Prediccion prediccion) {
        GridPane datos = new GridPane();
        datos.setHgap(22);
        datos.setVgap(6);
        datos.getStyleClass().add("tarjeta-datos");

        agregarDato(datos, 0, 0, "Marcador probable", prediccion.resultadoMasProbable());
        agregarDato(datos, 1, 0, "Goles esperados", prediccion.goles());
        agregarDato(datos, 2, 0, "Más de 2,5 goles", Prediccion.porcentaje(prediccion.probMasDe2()));
        agregarDato(datos, 0, 1, "Córners", prediccion.corners());
        agregarDato(datos, 1, 1, "Amarillas", prediccion.amarillas());
        agregarDato(datos, 2, 1, "Remates", prediccion.remates());
        agregarDato(datos, 3, 1, "Faltas", prediccion.faltas());
        return datos;
    }

    /** Pone en la rejilla un nombre pequeño en gris y su valor debajo. */
    private static void agregarDato(GridPane rejilla, int columna, int fila, String nombre, String valor) {
        Label etiqueta = new Label(nombre);
        etiqueta.getStyleClass().add("dato-nombre");
        Label numero = new Label(valor);
        numero.getStyleClass().add("dato-valor");
        rejilla.add(new VBox(1, etiqueta, numero), columna, fila);
    }

    /** "Sábado 29/11". */
    private static String textoFecha(LocalDate dia) {
        String nombreDia = dia.getDayOfWeek().getDisplayName(TextStyle.FULL, ESPANIA);
        // La primera letra en mayúscula: "sábado" → "Sábado"
        nombreDia = nombreDia.substring(0, 1).toUpperCase(ESPANIA) + nombreDia.substring(1);
        return nombreDia + " " + dia.format(DIA_MES);
    }
}
