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

/** Interpreta un CSV tipo Football-Data de la liga española SP1. */
public final class CsvPartidosParser {
    private static final DateTimeFormatter FECHA_LARGA =
            DateTimeFormatter.ofPattern("d/M/uuuu");
    private static final DateTimeFormatter FECHA_CORTA =
            DateTimeFormatter.ofPattern("d/M/uu");

    public List<PartidoHistorico> leer(Path archivo) throws IOException {
        try (Reader reader = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            return leer(reader);
        }
    }

    public List<PartidoHistorico> leer(Reader reader) throws IOException {
        BufferedReader buffered = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
        buffered.mark(1);
        if (buffered.read() != '\uFEFF') buffered.reset();
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setIgnoreSurroundingSpaces(true).build();

        List<PartidoHistorico> resultado = new ArrayList<>();
        try (CSVParser parser = formato.parse(buffered)) {
            List<String> cabeceras = parser.getHeaderNames();
            for (String obligatoria : List.of("Date", "HomeTeam", "AwayTeam", "FTHG", "FTAG")) {
                if (!cabeceras.contains(obligatoria)) {
                    throw new IOException("Falta la columna obligatoria: " + obligatoria);
                }
            }
            for (CSVRecord fila : parser) {
                try {
                    if (texto(fila, "HomeTeam").isBlank() || texto(fila, "AwayTeam").isBlank()) continue;
                    // El archivo puede contener huecos de partidos todavía sin disputar: no importarlos.
                    if (texto(fila, "FTHG").isBlank() || texto(fila, "FTAG").isBlank()) continue;
                    if (fila.isMapped("Div") && !texto(fila, "Div").isBlank()
                            && !texto(fila, "Div").equals("SP1")) continue;
                    String fecha = texto(fila, "Date");
                    LocalDate dia = fecha.length() >= 10
                            ? LocalDate.parse(fecha, FECHA_LARGA)
                            : LocalDate.parse(fecha, FECHA_CORTA);
                    resultado.add(new PartidoHistorico(
                            dia, texto(fila, "HomeTeam"), texto(fila, "AwayTeam"),
                            Integer.parseInt(texto(fila, "FTHG")),
                            Integer.parseInt(texto(fila, "FTAG")),
                            numero(fila, "HS"), numero(fila, "AS"),
                            numero(fila, "HST"), numero(fila, "AST"),
                            numero(fila, "HC"), numero(fila, "AC"),
                            numero(fila, "HY"), numero(fila, "AY"),
                            numero(fila, "HR"), numero(fila, "AR"),
                            numero(fila, "HF"), numero(fila, "AF")));
                } catch (RuntimeException error) {
                    throw new IOException("CSV inválido en línea " + fila.getRecordNumber()
                            + ": " + error.getMessage(), error);
                }
            }
        }
        if (resultado.isEmpty()) throw new IOException("No se encontraron partidos terminados.");
        return resultado;
    }

    private static String texto(CSVRecord fila, String campo) {
        if (!fila.isMapped(campo) || !fila.isSet(campo)) return "";
        return fila.get(campo).trim();
    }

    private static Integer numero(CSVRecord fila, String campo) {
        String valor = texto(fila, campo);
        return valor.isBlank() ? null : Integer.parseInt(valor);
    }
}
