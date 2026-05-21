import java.sql.*;

public class DataPurge {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/sigeclin";
        String user = "admin";
        String password = "admin";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println(">>> [Purge] Iniciando purga de datos...");
            Statement stmt = conn.createStatement();
            
            // Truncate tables with CASCADE
            String[] tables = {
                "clinico.auditoria_acceso",
                "seguridad.sesion_log",
                "clinico.pago_log",
                "clinico.atencion_preventiva",
                "clinico.inmunizacion",
                "clinico.dispensacion",
                "clinico.resultado_laboratorio",
                "clinico.orden_medica",
                "clinico.detalle_receta",
                "clinico.receta_medica",
                "clinico.diagnostico_consulta",
                "clinico.consulta",
                "clinico.triaje",
                "clinico.cita",
                "clinico.antecedente_paciente",
                "clinico.alergia_paciente"
            };

            for (String table : tables) {
                try {
                    stmt.execute("TRUNCATE " + table + " CASCADE");
                    System.out.println("Purged: " + table);
                } catch (Exception e) {
                    System.err.println("Failed to purge " + table + ": " + e.getMessage());
                }
            }

            // Delete patients but keep users/personal
            stmt.execute("DELETE FROM filiacion.paciente");
            System.out.println("Purged: filiacion.paciente");
            
            stmt.execute("DELETE FROM filiacion.persona WHERE id_persona NOT IN (SELECT id_usuario FROM filiacion.usuario) AND id_persona NOT IN (SELECT id_personal FROM filiacion.personal)");
            System.out.println("Purged: filiacion.persona (orphans)");

            System.out.println(">>> [Purge] Purga completada exitosamente.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
