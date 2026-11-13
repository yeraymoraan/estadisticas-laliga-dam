package es.fraydiegotadeo.laliga.importer;

import es.fraydiegotadeo.laliga.model.PartidoHistorico;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee los archivos CSV de Primera División (código SP1) de football-data.co.uk
 * y los convierte en una lista de objetos PartidoHistorico.
 *
 * Usa la librería Apache Commons CSV (declarada en el pom.xml) para separar las columnas,
 * porque un CSV puede tener comillas y comas dentro de un campo y hacerlo a mano es fácil que falle.
 *
 * Columnas de football-data que usamos: Date, HomeTeam, AwayTeam, FTHG y FTAG (goles al final
 * del partido) y, si existen, HS/AS (remates), HST/AST (a puerta), HC/AC (córners),
 * HY/AY (amarillas), HR/AR (rojas) y HF/AF (faltas). La H es "home" (local) y la A "away" (visitante).
 */
public final class CsvPartidosParser {

    /** Columnas sin las que no se puede importar un partido. */
    private static final List<String> COLUMNAS_OBLIGATORIAS =
            List.of("Date", "HomeTeam", "AwayTeam", "FTHG", "FTAG");

    // Football-Data escribe la fecha con el año completo (16/08/2025) o con dos cifras (16/08/25)
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("d/M/uuuu");
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("d/M/uu");

    /** Marca BOM: carácter invisible que algunos programas ponen al principio de los archivos UTF-8. */
    private static final int BOM = '﻿';

    /**
     * Lee un archivo CSV del disco.
     *
     * @param archivo ruta del CSV
     * @return partidos terminados que contiene el archivo
     * @throws IOException si no se puede leer o el formato no es válido
     */
    public List<PartidoHistorico> leer(Path archivo) throws IOException {
        // try-with-resources: el archivo se cierra solo al terminar, aunque haya un error
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            return leer(lector);
        }
    }

    /**
     * Lee el contenido CSV desde cualquier Reader. Existe para poder probar el parser
     * con un texto en memoria (StringReader) sin necesitar un archivo real.
     */
    public List<PartidoHistorico> leer(Reader lector) throws IOException {
        BufferedReader buffer = new BufferedReader(lector);
        saltarBom(buffer);

        // Formato: la primera fila es la cabecera y se ignoran espacios alrededor de cada valor
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        List<PartidoHistorico> partidos = new ArrayList<>();
        try (CSVParser parser = formato.parse(buffer)) {
            comprobarCabeceras(parser.getHeaderNames());

            for (CSVRecord fila : parser) {
                PartidoHistorico partido = convertirFila(fila);
                if (partido != null) {
                    partidos.add(partido);
                }
            }
        }

        if (partidos.isEmpty()) {
            throw new IOException("No se encontraron partidos terminados.");
        }
        return partidos;
    }

    /** Si el archivo empieza por la marca BOM se la salta; si no, vuelve al principio. */
    private static void saltarBom(BufferedReader buffer) throws IOException {
        buffer.mark(1);
        if (buffer.read() != BOM) {
            buffer.reset();
        }
    }

    private static void comprobarCabeceras(List<String> cabeceras) throws IOException {
        for (String obligatoria : COLUMNAS_OBLIGATORIAS) {
            if (!cabeceras.contains(obligatoria)) {
                throw new IOException("Falta la columna obligatoria: " + obligatoria);
            }
        }
    }

    /**
     * Convierte una fila del CSV en un partido.
     *
     * @return el partido, o null si la fila se debe ignorar (vacía, sin resultado o de otra división)
     * @throws IOException si la fila tiene datos incorrectos (fecha mal escrita, goles negativos...)
     */
    private static PartidoHistorico convertirFila(CSVRecord fila) throws IOException {
        // Algunas temporadas antiguas tienen filas vacías al final del archivo
        if (texto(fila, "HomeTeam").isBlank() || texto(fila, "AwayTeam").isBlank()) {
            return null;
        }
        // Partido todavía sin disputar: no tiene goles y no se importa
        if (texto(fila, "FTHG").isBlank() || texto(fila, "FTAG").isBlank()) {
            return null;
        }
        // Solo Primera División (SP1). Si no existe la columna Div damos la fila por buena.
        String division = texto(fila, "Div");
        if (!division.isBlank() && !division.equals("SP1")) {
            return null;
        }

        try {
            return new PartidoHistorico(
                    leerFecha(texto(fila, "Date")),
                    texto(fila, "HomeTeam"),
                    texto(fila, "AwayTeam"),
                    Integer.parseInt(texto(fila, "FTHG")),
                    Integer.parseInt(texto(fila, "FTAG")),
                    numero(fila, "HS"), numero(fila, "AS"),
                    numero(fila, "HST"), numero(fila, "AST"),
                    numero(fila, "HC"), numero(fila, "AC"),
                    numero(fila, "HY"), numero(fila, "AY"),
                    numero(fila, "HR"), numero(fila, "AR"),
                    numero(fila, "HF"), numero(fila, "AF"));
        } catch (RuntimeException error) {
            // Aquí llegan los errores de parseInt, de la fecha y de las validaciones de PartidoHistorico.
            // Los convertimos en IOException indicando la línea para que el usuario pueda revisarla.
            throw new IOException("CSV inválido en línea " + fila.getRecordNumber()
                    + ": " + error.getMessage(), error);
        }
    }

    /** Convierte "16/08/2025" o "16/08/25" en un LocalDate. */
    private static LocalDate leerFecha(String fecha) {
        if (fecha.length() >= 10) {
            return LocalDate.parse(fecha, FECHA_LARGA);
        }
        return LocalDate.parse(fecha, FECHA_CORTA);
    }

    /** Devuelve el texto de una columna, o "" si la columna no existe o está vacía. */
    private static String texto(CSVRecord fila, String columna) {
        if (!fila.isMapped(columna) || !fila.isSet(columna)) {
            return "";
        }
        return fila.get(columna).trim();
    }

    /** Devuelve el número de una columna, o null si no hay dato (nunca un 0 inventado). */
    private static Integer numero(CSVRecord fila, String columna) {
        String valor = texto(fila, columna);
        if (valor.isBlank()) {
            return null;
        }
        return Integer.parseInt(valor);
    }
}
