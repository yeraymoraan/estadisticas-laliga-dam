package es.fraydiegotadeo.laliga.repository;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Abre conexiones JDBC con la base de datos MySQL.
 *
 * Los datos de acceso nunca se escriben en el código ni se suben a GitHub. Se buscan en este orden:
 * 1. Variables de entorno de Windows: LALIGA_DB_URL, LALIGA_DB_USER y LALIGA_DB_PASSWORD (ver .env.example).
 * 2. El archivo laliga.properties de la carpeta donde se ejecuta el programa, con las claves
 *    url, usuario y password. Es más cómodo para quien instala la aplicación desde el ZIP.
 */
public final class ConexionMySql {

    /** URL que se usa si no se ha definido ninguna. */
    private static final String URL_POR_DEFECTO =
            "jdbc:mysql://localhost:3306/laliga_dam?serverTimezone=Europe/Madrid";

    /** Archivo de configuración opcional, en la carpeta desde la que se ejecuta el programa. */
    private static final Path ARCHIVO_CONFIGURACION = Path.of("laliga.properties");

    // Constructor privado: esta clase solo tiene métodos estáticos y no se crean objetos de ella
    private ConexionMySql() {
    }

    /**
     * Abre una conexión nueva. Quien la pide debe cerrarla (lo hacemos con try-with-resources).
     *
     * @return conexión abierta con MySQL
     * @throws SQLException si falta el usuario o MySQL no responde
     */
    public static Connection abrir() throws SQLException {
        Properties archivo = leerArchivo(ARCHIVO_CONFIGURACION);

        String usuario = valor("LALIGA_DB_USER", "usuario", archivo);
        if (usuario == null || usuario.isBlank()) {
            throw new SQLException("Falta el usuario de MySQL: configura LALIGA_DB_USER o el archivo laliga.properties.");
        }

        String url = valor("LALIGA_DB_URL", "url", archivo);
        if (url == null || url.isBlank()) {
            url = URL_POR_DEFECTO;
        }

        String password = valor("LALIGA_DB_PASSWORD", "password", archivo);
        if (password == null) {
            password = "";
        }

        return DriverManager.getConnection(url, usuario, password);
    }

    /**
     * Valor de un dato de conexión: primero la variable de entorno y, si no existe, la clave del archivo.
     *
     * @return el valor, o null si no está en ninguno de los dos sitios
     */
    static String valor(String variableEntorno, String claveArchivo, Properties archivo) {
        String desdeEntorno = System.getenv(variableEntorno);
        if (desdeEntorno != null && !desdeEntorno.isBlank()) {
            return desdeEntorno;
        }
        return archivo.getProperty(claveArchivo);
    }

    /**
     * Lee un archivo .properties (líneas "clave=valor"). Properties es una clase de Java pensada
     * justo para esto. Si el archivo no existe o no se puede leer, devuelve uno vacío.
     */
    static Properties leerArchivo(Path ruta) {
        Properties propiedades = new Properties();
        if (!Files.exists(ruta)) {
            return propiedades;
        }
        try (Reader lector = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            propiedades.load(lector);
        } catch (IOException error) {
            // Un archivo que no se puede leer se trata como si no existiera; abrir() avisará si falta el usuario
            return new Properties();
        }
        return propiedades;
    }
}
