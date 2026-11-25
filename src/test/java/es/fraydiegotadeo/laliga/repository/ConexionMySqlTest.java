package es.fraydiegotadeo.laliga.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de la lectura de laliga.properties (sin conectarse a MySQL). */
class ConexionMySqlTest {

    @Test
    void leeLasClavesDelArchivo(@TempDir Path carpeta) throws Exception {
        // @TempDir: JUnit crea una carpeta temporal para la prueba y la borra al terminar
        Path archivo = carpeta.resolve("laliga.properties");
        Files.writeString(archivo, "usuario=profesor\npassword=prueba\n");

        Properties propiedades = ConexionMySql.leerArchivo(archivo);

        assertEquals("profesor", propiedades.getProperty("usuario"));
        assertEquals("prueba", propiedades.getProperty("password"));
        assertNull(propiedades.getProperty("url"));
    }

    @Test
    void sinArchivoDevuelvePropiedadesVacias(@TempDir Path carpeta) {
        Properties propiedades = ConexionMySql.leerArchivo(carpeta.resolve("no-existe.properties"));

        assertTrue(propiedades.isEmpty());
    }

    @Test
    void siNoHayVariableDeEntornoSeUsaElArchivo() {
        Properties archivo = new Properties();
        archivo.setProperty("usuario", "profesor");

        // Una variable que seguro que no existe en el ordenador donde se ejecuta la prueba
        assertEquals("profesor", ConexionMySql.valor("LALIGA_VARIABLE_QUE_NO_EXISTE", "usuario", archivo));
    }
}
