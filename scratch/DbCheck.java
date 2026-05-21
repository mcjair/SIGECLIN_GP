// package com.sigeclin;
import java.sql.*;
public class DbCheck {
    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/sigeclin", "admin", "admin")) {
            System.out.println("--- PERSONAS ---");
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT id_persona, nombres, numero_documento FROM filiacion.persona WHERE id_persona >= 100")) {
                while (rs.next()) {
                    System.out.println(rs.getInt(1) + ": " + rs.getString(2) + " (" + rs.getString(3) + ")");
                }
            }
            System.out.println("--- PERSONAL ---");
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT id_personal, id_tipo_personal, numero_colegiatura FROM filiacion.personal WHERE id_personal >= 100")) {
                while (rs.next()) {
                    System.out.println(rs.getInt(1) + ": Type=" + rs.getInt(2) + ", Coleg=" + rs.getString(3));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
