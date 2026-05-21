import java.sql.*;

public class ListUsers {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/postgres"; // Try postgres db
        String user = "postgres";
        String password = "admin"; // Try standard password

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println(">>> Connected as postgres!");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT usename FROM pg_user");
            System.out.println("Users in PostgreSQL:");
            while (rs.next()) {
                System.out.println(" - " + rs.getString("usename"));
            }
        } catch (Exception e) {
            System.err.println("Error as postgres: " + e.getMessage());
            // Try with no password
            try (Connection conn = DriverManager.getConnection(url, user, "")) {
                System.out.println(">>> Connected as postgres (no password)!");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT usename FROM pg_user");
                System.out.println("Users in PostgreSQL:");
                while (rs.next()) {
                    System.out.println(" - " + rs.getString("usename"));
                }
            } catch (Exception e2) {
                System.err.println("Error as postgres (no password): " + e2.getMessage());
            }
        }
    }
}
