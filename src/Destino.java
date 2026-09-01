// Formato de salida del codigo generado.
public enum Destino {

    POSTGRESQL("PostgreSQL", "sql", true),
    MYSQL("MySQL / MariaDB", "sql", true),
    SQLSERVER("SQL Server", "sql", true),
    SQLITE("SQLite", "sql", true),
    SQLALCHEMY("Python - SQLAlchemy", "py", false),
    TYPESCRIPT("TypeScript - interfaces", "ts", false);

    private final String etiqueta;
    private final String extension;
    private final boolean esSQL;

    Destino(String etiqueta, String extension, boolean esSQL) {
        this.etiqueta = etiqueta;
        this.extension = extension;
        this.esSQL = esSQL;
    }

    // Nombre legible, para el desplegable de la interfaz.
    public String getEtiqueta() {
        return etiqueta;
    }

    // Extension del archivo al exportar, sin el punto.
    public String getExtension() {
        return extension;
    }

    public boolean esSQL() {
        return esSQL;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
