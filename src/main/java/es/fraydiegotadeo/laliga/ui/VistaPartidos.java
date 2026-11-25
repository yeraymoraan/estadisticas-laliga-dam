package es.fraydiegotadeo.laliga.ui;

import es.fraydiegotadeo.laliga.model.NombresEquipos;
import es.fraydiegotadeo.laliga.model.PartidoVista;
import es.fraydiegotadeo.laliga.service.JornadasService;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla "Partidos": los partidos de una temporada organizados por jornadas.
 * Si se escribe un equipo en el buscador se ven todos sus partidos de la temporada.
 */
public final class VistaPartidos {

    private final TableView<PartidoVista> tabla = new TableView<>();
    private final TextField buscador = new TextField();
    private final ComboBox<Integer> selectorJornada = new ComboBox<>();
    private final Button anterior = new Button("‹");
    private final Button siguiente = new Button("›");
    private final Label resumen = new Label();
    private final VBox raiz;

    /** Jornadas de la temporada y, para cada partido, el número de su jornada. */
    private List<JornadasService.Jornada> jornadas = new ArrayList<>();
    private final Map<PartidoVista, Integer> jornadaDePartido = new HashMap<>();
    private List<PartidoVista> todos = new ArrayList<>();

    public VistaPartidos() {
        Tablas.agregarColumnaCentrada(tabla, "J", partido -> String.valueOf(jornadaDePartido.get(partido)), 45);
        Tablas.agregarColumna(tabla, "FECHA", partido -> Tablas.fecha(partido.fecha()), 100);
        Tablas.agregarColumna(tabla, "LOCAL", partido -> NombresEquipos.completo(partido.local()), 175);
        Tablas.agregarColumnaCentrada(tabla, "RESULTADO", PartidoVista::resultado, 95)
                .getStyleClass().add("celda-negrita");
        Tablas.agregarColumna(tabla, "VISITANTE", partido -> NombresEquipos.completo(partido.visitante()), 175);
        Tablas.agregarColumnaCentrada(tabla, "REMATES", PartidoVista::remates, 90);
        Tablas.agregarColumnaCentrada(tabla, "A PUERTA", PartidoVista::rematesPuerta, 90);
        Tablas.agregarColumnaCentrada(tabla, "CÓRNERS", PartidoVista::corners, 90);
        Tablas.agregarColumnaCentrada(tabla, "AMARILLAS", PartidoVista::amarillas, 90);
        Tablas.agregarColumnaCentrada(tabla, "ROJAS", PartidoVista::rojas, 80);
        Tablas.agregarColumnaCentrada(tabla, "FALTAS", PartidoVista::faltas, 85);
        tabla.setPlaceholder(new Label("Cargando partidos..."));
        Tablas.ajustarAlAncho(tabla);

        Label aviso = new Label("La jornada se calcula con los partidos que lleva cada equipo; "
                + "un partido aplazado aparece en la jornada en la que se jugó.");
        aviso.getStyleClass().add("texto-suave");

        raiz = new VBox(12, crearBarra(), tabla, aviso);
        VBox.setVgrow(tabla, Priority.ALWAYS);
    }

    /** Barra de arriba: ‹ Jornada 13 › a la izquierda y el buscador a la derecha. */
    private HBox crearBarra() {
        // El selector guarda el número (13) pero enseña "Jornada 13"
        selectorJornada.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer numero) {
                if (numero == null) {
                    return "";
                }
                return "Jornada " + numero;
            }

            @Override
            public Integer fromString(String texto) {
                return null; // No se usa: el selector no deja escribir
            }
        });
        selectorJornada.setOnAction(evento -> filtrar());
        anterior.getStyleClass().add("boton-flecha");
        siguiente.getStyleClass().add("boton-flecha");
        anterior.setOnAction(evento -> moverJornada(-1));
        siguiente.setOnAction(evento -> moverJornada(1));

        buscador.setPromptText("Buscar equipo...");
        buscador.getStyleClass().add("buscador");
        buscador.setPrefWidth(280);
        // Listener: cada vez que cambia el texto del buscador se vuelve a filtrar la tabla
        buscador.textProperty().addListener((propiedad, textoAnterior, textoNuevo) -> filtrar());

        resumen.getStyleClass().add("texto-suave");
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        HBox barra = new HBox(8, anterior, selectorJornada, siguiente, resumen, hueco, buscador);
        barra.setAlignment(Pos.CENTER_LEFT);
        return barra;
    }

    /** @return el contenedor de la pantalla, para ponerlo en la ventana */
    public Node getRaiz() {
        return raiz;
    }

    /** Recibe los partidos de una temporada, los reparte en jornadas y enseña la última. */
    public void mostrar(List<PartidoVista> partidosTemporada) {
        todos = partidosTemporada;
        jornadas = JornadasService.agrupar(partidosTemporada);
        jornadaDePartido.clear();
        List<Integer> numeros = new ArrayList<>();
        for (JornadasService.Jornada jornada : jornadas) {
            numeros.add(jornada.numero());
            for (PartidoVista partido : jornada.partidos()) {
                jornadaDePartido.put(partido, jornada.numero());
            }
        }
        selectorJornada.getItems().setAll(numeros);
        if (!numeros.isEmpty()) {
            // Por defecto la última jornada jugada, que es la que más interesa
            selectorJornada.setValue(numeros.get(numeros.size() - 1));
        }
        filtrar();
    }

    /** Pasa a la jornada anterior (-1) o a la siguiente (+1), si existe. */
    private void moverJornada(int salto) {
        int posicion = selectorJornada.getItems().indexOf(selectorJornada.getValue()) + salto;
        if (posicion >= 0 && posicion < selectorJornada.getItems().size()) {
            selectorJornada.setValue(selectorJornada.getItems().get(posicion));
        }
    }

    /**
     * Sin texto en el buscador se ven los partidos de la jornada elegida.
     * Con texto se ven todos los partidos de la temporada de los equipos que coinciden.
     */
    private void filtrar() {
        String filtro = buscador.getText() == null ? "" : buscador.getText().trim().toLowerCase();
        List<PartidoVista> visibles = new ArrayList<>();

        if (filtro.isEmpty()) {
            Integer numero = selectorJornada.getValue();
            for (JornadasService.Jornada jornada : jornadas) {
                if (numero != null && jornada.numero() == numero) {
                    visibles.addAll(jornada.partidos());
                }
            }
            resumen.setText(visibles.size() + " partidos");
        } else {
            for (PartidoVista partido : todos) {
                // Se busca en el nombre completo ("Atlético") y en el de Football-Data ("Ath Madrid")
                String local = partido.local() + " " + NombresEquipos.completo(partido.local());
                String visitante = partido.visitante() + " " + NombresEquipos.completo(partido.visitante());
                if (local.toLowerCase().contains(filtro) || visitante.toLowerCase().contains(filtro)) {
                    visibles.add(partido);
                }
            }
            resumen.setText(visibles.size() + " partidos en toda la temporada");
        }

        // Mientras se busca un equipo no tiene sentido cambiar de jornada
        selectorJornada.setDisable(!filtro.isEmpty());
        anterior.setDisable(!filtro.isEmpty());
        siguiente.setDisable(!filtro.isEmpty());
        tabla.setItems(FXCollections.observableArrayList(visibles));
    }
}
