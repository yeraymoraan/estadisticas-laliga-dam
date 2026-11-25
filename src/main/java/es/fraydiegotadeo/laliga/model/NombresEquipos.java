package es.fraydiegotadeo.laliga.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Convierte los nombres abreviados de Football-Data ("Ath Madrid", "Sociedad"...) en los nombres
 * completos que se muestran en la interfaz ("Atlético de Madrid", "Real Sociedad"...).
 *
 * En la base de datos se siguen guardando los nombres de Football-Data, porque son los que vienen
 * en los CSV y así se pueden cruzar los resultados con el calendario. Solo se cambian al mostrarlos.
 */
public final class NombresEquipos {

    /** Nombre de Football-Data → nombre completo. Los que no están se muestran tal cual. */
    private static final Map<String, String> NOMBRES = new HashMap<>();

    // Bloque static: se ejecuta una sola vez, cuando se usa la clase por primera vez
    static {
        NOMBRES.put("Alaves", "Alavés");
        NOMBRES.put("Almeria", "Almería");
        NOMBRES.put("Ath Bilbao", "Athletic Club");
        NOMBRES.put("Ath Madrid", "Atlético de Madrid");
        NOMBRES.put("Betis", "Real Betis");
        NOMBRES.put("Cadiz", "Cádiz");
        NOMBRES.put("Celta", "Celta de Vigo");
        NOMBRES.put("Cordoba", "Córdoba");
        NOMBRES.put("Espanol", "Espanyol");
        NOMBRES.put("Gimnastic", "Gimnàstic de Tarragona");
        NOMBRES.put("Hercules", "Hércules");
        NOMBRES.put("La Coruna", "Deportivo de La Coruña");
        NOMBRES.put("Leganes", "Leganés");
        NOMBRES.put("Lerida", "Lleida");
        NOMBRES.put("Logrones", "Logroñés");
        NOMBRES.put("Malaga", "Málaga");
        NOMBRES.put("Merida", "Mérida");
        NOMBRES.put("Oviedo", "Real Oviedo");
        NOMBRES.put("Recreativo", "Recreativo de Huelva");
        NOMBRES.put("Santander", "Racing de Santander");
        NOMBRES.put("Sociedad", "Real Sociedad");
        NOMBRES.put("Sp Gijon", "Sporting de Gijón");
        NOMBRES.put("Vallecano", "Rayo Vallecano");
        // En la temporada 1998/99 Football-Data lo escribe con una sola "r" (ver INC-09)
        NOMBRES.put("Villareal", "Villarreal");
    }

    private NombresEquipos() {
    }

    /**
     * @param nombreFootballData nombre tal como viene en los CSV
     * @return nombre completo para mostrar, o el mismo si no hay traducción
     */
    public static String completo(String nombreFootballData) {
        String completo = NOMBRES.get(nombreFootballData);
        if (completo == null) {
            return nombreFootballData;
        }
        return completo;
    }
}
