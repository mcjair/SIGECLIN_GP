package com.sigeclin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

public class TestDB {
    public static void main(String[] args) {
        try {
            Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/sigeclin", "admin", "admin");
            ResultSet rs = c.createStatement().executeQuery("SELECT * FROM filiacion.usuario ORDER BY id_usuario DESC LIMIT 5");
            System.out.println("--- ULTIMOS USUARIOS ---");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id_usuario") + " | Username: " + rs.getString("username") + " | Registro: " + rs.getTimestamp("fecha_creacion"));
            }
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
