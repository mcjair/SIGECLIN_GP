import java.sql.*;

public class QuerySchema {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://127.0.0.1:5432/sigeclin?sslmode=disable";
        String user = "admin";
        String password = "admin";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to sigeclin!");
            
            printTableColumns(conn, "clinico", "detalle_receta");
            printTableColumns(conn, "clinico", "receta_medica");
            printTableColumns(conn, "clinico", "referencia");
            printTableColumns(conn, "clinico", "referencia_medica");
            printTableColumns(conn, "clinico", "consulta");

            System.out.println("--- LIST OF TABLES IN CLINICO SCHEMA ---");
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, "clinico", "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    System.out.println("Table: " + rs.getString("TABLE_NAME"));
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printTableColumns(Connection conn, String schema, String table) throws SQLException {
        System.out.println("--- COLUMNS FOR " + schema + "." + table + " ---");
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, schema, table, null)) {
            boolean found = false;
            while (rs.next()) {
                found = true;
                String name = rs.getString("COLUMN_NAME");
                String type = rs.getString("TYPE_NAME");
                System.out.println("  " + name + " (" + type + ")");
            }
            if (!found) {
                System.out.println("  (Table not found or no columns)");
            }
        }
    }
}
