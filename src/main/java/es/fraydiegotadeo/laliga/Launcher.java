package es.fraydiegotadeo.laliga;

/**
 * Punto de entrada que NO hereda de Application. Si la clase con main hereda de
 * Application y JavaFX está en el classpath, Java aborta con "JavaFX runtime
 * components are missing"; con esta clase intermedia se puede ejecutar y depurar
 * desde el botón Run/Debug de IntelliJ y desde un JAR.
 */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) { App.main(args); }
}
