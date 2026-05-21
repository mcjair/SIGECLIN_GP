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
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class Cie10Seeder implements CommandLineRunner {

    private final Cie10Repository cie10Repository;

    @Override
    public void run(String... args) throws Exception {
        if (cie10Repository.count() > 0) {
            log.info("SIGECLIN: Catálogo CIE-10 ya inicializado.");
            return;
        }

        String csvPath = "D:/UTP/SISTEMAS/AEAMAN/ciex/diagnosticos_cie10.csv";
        log.info("SIGECLIN: Cargando catálogo CIE-10 desde {}...", csvPath);

        List<Cie10> batch = new ArrayList<>();
        int count = 0;
        
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath, StandardCharsets.UTF_8))) {
            String line;
            br.readLine(); // Skip header
            
            while ((line = br.readLine()) != null) {
                // Simple CSV split (not handling commas inside quotes perfectly, but enough for this format)
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length >= 2) {
                    Cie10 c = new Cie10();
                    c.setCodigo(parts[0].replace("\"", "").trim());
                    c.setDescripcion(parts[1].replace("\"", "").trim());
                    c.setActivo(parts.length > 6 && parts[6].contains("ACTIVO"));
                    
                    batch.add(c);
                    count++;
                    
                    if (batch.size() >= 500) {
                        cie10Repository.saveAll(batch);
                        batch.clear();
                        if (count % 2000 == 0) log.info("SIGECLIN: {} diagnósticos cargados...", count);
                    }
                }
                if (count >= 10000) break; // Limit to 10k for performance in dev
            }
            if (!batch.isEmpty()) {
                cie10Repository.saveAll(batch);
            }
        } catch (Exception e) {
            log.error("Error cargando CIE-10: {}", e.getMessage());
        }

        log.info("SIGECLIN: Carga de CIE-10 completada. Total: {} registros.", count);
    }
}
