package es.fraydiegotadeo.laliga.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Abre conexiones JDBC con la base de datos MySQL.
 *
 * Los datos de acceso se leen de variables de entorno de Windows para no escribir nunca
 * la contraseña en el código ni subirla a GitHub:
 * LALIGA_DB_URL, LALIGA_DB_USER y LALIGA_DB_PASSWORD (ver .env.example).
 */
public final class ConexionMySql {

    /** URL que se usa si no se ha definido LALIGA_DB_URL. */
    private static final String URL_POR_DEFECTO =
            "jdbc:mysql://localhost:3306/laliga_dam?serverTimezone=Europe/Madrid";

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
        String usuario = System.getenv("LALIGA_DB_USER");
        if (usuario == null || usuario.isBlank()) {
            throw new SQLException("Configura LALIGA_DB_USER antes de iniciar la aplicación.");
        }

        String url = System.getenv("LALIGA_DB_URL");
        if (url == null || url.isBlank()) {
            url = URL_POR_DEFECTO;
        }

        String password = System.getenv("LALIGA_DB_PASSWORD");
        if (password == null) {
            password = "";
        }

        return DriverManager.getConnection(url, usuario, password);
    }
}
