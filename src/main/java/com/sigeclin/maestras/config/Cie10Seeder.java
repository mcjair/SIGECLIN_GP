package com.sigeclin.maestras.config;

import com.sigeclin.maestras.model.Cie10;
import com.sigeclin.maestras.repository.Cie10Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class Cie10Seeder implements CommandLineRunner {

    private final Cie10Repository cie10Repository;

    @Override
    public void run(String... args) throws Exception {
        if (cie10Repository.count() > 0) {
            log.info("SIGECLIN: Catálogo CIE-10 ya inicializado ({} registros).", cie10Repository.count());
            return;
        }

        String csvPath = "D:/UTP/SISTEMAS/AEAMAN/ciex/diagnosticos_cie10.csv";
        log.info("SIGECLIN: Cargando catálogo CIE-10 CURADO (~340 códigos) desde {}...", csvPath);

        List<Cie10> batch = new ArrayList<>();
        int count = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath, StandardCharsets.UTF_8))) {
            String line;
            String header = br.readLine(); // Skip header

            while ((line = br.readLine()) != null) {
                try {
                    if (line.trim().isEmpty()) continue;

                    // Parse CSV con soporte de comillas
                    List<String> parts = new ArrayList<>();
                    boolean inQuotes = false;
                    StringBuilder sb = new StringBuilder();
                    for (char c : line.toCharArray()) {
                        if (c == '\"') {
                            inQuotes = !inQuotes;
                        } else if (c == ',' && !inQuotes) {
                            parts.add(sb.toString().trim());
                            sb = new StringBuilder();
                        } else {
                            sb.append(c);
                        }
                    }
                    parts.add(sb.toString().trim());

                    if (parts.size() >= 3) {
                        Cie10 c = new Cie10();
                        c.setCodigo(parts.get(0).replace("\"", "").trim());
                        c.setDescripcion(parts.get(1).replace("\"", "").trim());
                        c.setServicios(parts.get(2).replace("\"", "").trim());
                        c.setActivo(true);

                        batch.add(c);
                        count++;

                        if (batch.size() >= 50) {
                            cie10Repository.saveAll(batch);
                            batch.clear();
                        }
                    }
                } catch (Exception inner) {
                    // Skip malformed line
                }
            }
            if (!batch.isEmpty()) {
                cie10Repository.saveAll(batch);
            }
        } catch (Exception e) {
            log.error("Error cargando CIE-10 CURADO: {}", e.getMessage());
        }

        log.info("SIGECLIN: Carga CURADA de CIE-10 completada. Total: {} registros esenciales.", count);
    }
}
