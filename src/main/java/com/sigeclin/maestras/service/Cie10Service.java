package com.sigeclin.maestras.service;

import com.sigeclin.maestras.model.Cie10;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class Cie10Service implements ICie10Service {

    @Value("${sigeclin.cie10.dir-path}")
    private String dirPath;

    private final List<Cie10> cie10Cache = new ArrayList<>();

    // Caché de Google Guava para almacenar los resultados de búsquedas CIE-10 (máx 1000 entradas, expira en 10 minutos de inactividad)
    private final com.google.common.cache.Cache<String, List<Cie10>> searchCache = com.google.common.cache.CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(10, java.util.concurrent.TimeUnit.MINUTES)
            .build();

    @PostConstruct
    public void init() {
        try {
            log.info("Cie10Service: Iniciando carga de caché CIE-10...");
            cargarCie10DesdeCsv();
        } catch (Exception e) {
            log.error("CRITICAL ERROR: No se pudo inicializar la caché CIE-10: {}", e.getMessage(), e);
        }
    }

    private void cargarCie10DesdeCsv() {
        File folder = new File(dirPath);
        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("Directorio CIE-10 no encontrado en la ruta configurada: {}. Intentando usar fallback relativo 'ciex'...", dirPath);
            folder = new File("ciex");
            if (!folder.exists() || !folder.isDirectory()) {
                log.error("CRITICAL: Directorio CIE-10 no encontrado ni en la ruta configurada ni en el fallback 'ciex'");
                return;
            }
        }

        cie10Cache.clear();
        searchCache.invalidateAll();

        File file = new File(folder, "diagnosticos_cie10.csv");
        if (!file.exists()) {
            log.error("CRITICAL: Archivo diagnosticos_cie10.csv no encontrado en: {}", file.getAbsolutePath());
            return;
        }

        int count = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            br.readLine(); // Saltar cabecera
            while ((line = br.readLine()) != null) {
                try {
                    if (line.trim().isEmpty()) continue;

                    String[] parts = parseCsvLine(line);
                    if (parts.length >= 3) {
                        String codigo = parts[0].replace("\"", "").trim();
                        String descripcion = parts[1].replace("\"", "").trim();
                        String servicios = parts.length >= 3 ? parts[2].replace("\"", "").trim() : "";

                        if (!codigo.isEmpty() && !descripcion.isEmpty()) {
                            Cie10 item = new Cie10();
                            item.setCodigo(codigo);
                            item.setDescripcion(descripcion);
                            item.setServicios(servicios);
                            cie10Cache.add(item);
                            count++;
                        }
                    }
                } catch (Exception inner) {
                    // Skip malformed line
                }
            }
        } catch (Exception e) {
            log.error("Error al cargar diagnosticos_cie10.csv: {}", e.getMessage());
        }
        log.info("=== CARGA CURADA FINALIZADA: {} diagnósticos esenciales en memoria ===", cie10Cache.size());
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString().trim());
        return result.toArray(new String[0]);
    }

    private String normalize(String str) {
        if (str == null) return "";
        return java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
    }

    public List<Cie10> search(String q) {
        return search(q, null);
    }

    public List<Cie10> search(String q, String servicio) {
        if (q == null || q.trim().isEmpty()) return new ArrayList<>();

        String normalizedQuery = normalize(q);
        try {
            String cacheKey = servicio != null ? servicio + ":" + normalizedQuery : normalizedQuery;
            return searchCache.get(cacheKey, () -> cie10Cache.stream()
                    .filter(c -> (c.getCodigo() != null && c.getCodigo().toLowerCase().contains(normalizedQuery)) ||
                                (c.getDescripcion() != null && normalize(c.getDescripcion()).contains(normalizedQuery)))
                    .filter(c -> servicio == null || c.getServicios() == null || c.getServicios().isEmpty() ||
                                c.getServicios().toUpperCase().contains("DIAGN") ||
                                c.getServicios().toUpperCase().contains("CIEX") ||
                                c.getServicios().toUpperCase().contains(servicio.toUpperCase()))
                    .limit(25)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error al consultar caché de Guava para query '{}': {}", q, e.getMessage());
            return cie10Cache.stream()
                    .filter(c -> (c.getCodigo() != null && c.getCodigo().toLowerCase().contains(normalizedQuery)) ||
                                (c.getDescripcion() != null && normalize(c.getDescripcion()).contains(normalizedQuery)))
                    .filter(c -> servicio == null || c.getServicios() == null || c.getServicios().isEmpty() ||
                                c.getServicios().toUpperCase().contains("DIAGN") ||
                                c.getServicios().toUpperCase().contains("CIEX") ||
                                c.getServicios().toUpperCase().contains(servicio.toUpperCase()))
                    .limit(25)
                    .collect(Collectors.toList());
        }
    }

    public int getCacheSize() {
        return cie10Cache.size();
    }
}
