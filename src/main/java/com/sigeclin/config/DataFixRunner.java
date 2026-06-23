package com.sigeclin.config;

import com.sigeclin.clinico.model.Consulta;
import com.sigeclin.filiacion.model.Personal;
import com.sigeclin.clinico.repository.ConsultaRepository;
import com.sigeclin.filiacion.repository.PersonalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class DataFixRunner implements CommandLineRunner {

    private final ConsultaRepository consultaRepository;
    private final PersonalRepository personalRepository;
    private static final Logger log = LoggerFactory.getLogger(DataFixRunner.class);

    public DataFixRunner(ConsultaRepository consultaRepository, PersonalRepository personalRepository) {
        this.consultaRepository = consultaRepository;
        this.personalRepository = personalRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("======================================================");
        log.info("[DATA FIX] Iniciando algoritmo de corrección de Historias Clínicas...");

        List<Consulta> consultas = consultaRepository.findAll();
        List<Personal> personalDb = personalRepository.findAll();

        int arreglados = 0;

        for (Consulta c : consultas) {
            if (c.getTriaje() != null && c.getTriaje().getServicioDestino() != null) {
                String servicio = c.getTriaje().getServicioDestino().toUpperCase();
                Personal asignadoOriginal = c.getMedico();

                Personal mejorOpcion = buscarMejorPersonal(servicio, personalDb);

                if (mejorOpcion != null && (asignadoOriginal == null || !asignadoOriginal.getIdPersona().equals(mejorOpcion.getIdPersona()))) {
                    // Forzar el cambio al verdadero especialista
                    c.setMedico(mejorOpcion);
                    
                    // Asegurar que el idEspecialidad en la consulta también cambie, y evitar nulos
                    Integer nuevaEsp = mejorOpcion.getIdEspecialidad() != null ? mejorOpcion.getIdEspecialidad() : getEspDeseada(servicio);
                    c.setIdEspecialidad(nuevaEsp);
                    
                    consultaRepository.save(c);
                    arreglados++;
                    log.info("[DATA FIX] Consulta {} (Servicio: {}) corregida -> Médico asignado: {} {} (Esp: {})",
                            c.getIdConsulta(), servicio, mejorOpcion.getNombres(), mejorOpcion.getApellidoPaterno(), nuevaEsp);
                }
            }
        }

        log.info("[DATA FIX] Migración algorítmica finalizada. {} atenciones históricas fueron arregladas definitivamente.", arreglados);
        log.info("======================================================");
    }

    private int getEspDeseada(String servicio) {
        if (servicio.contains("OBST")) return 2;
        if (servicio.contains("ODONT")) return 3;
        if (servicio.contains("PSIC")) return 4;
        if (servicio.contains("NUTR")) return 5;
        return 1;
    }

    private Personal buscarMejorPersonal(String servicio, List<Personal> personalDb) {
        final int targetEsp = getEspDeseada(servicio);

        // 1er intento: Buscar por código exacto de especialidad
        Optional<Personal> porCodigo = personalDb.stream()
                .filter(p -> p.getIdEspecialidad() != null && p.getIdEspecialidad() == targetEsp)
                .findFirst();
        if (porCodigo.isPresent()) return porCodigo.get();

        // 2do intento: Buscar por palabras clave en los nombres (Ej. si registraron a Elizabeth)
        if (servicio.contains("NUTR")) {
            return personalDb.stream().filter(p -> p.getNombres().toUpperCase().contains("ELIZABETH")).findFirst().orElse(null);
        }
        if (servicio.contains("PSIC")) {
            // Busca al psicólogo por palabras clave comunes o cualquier personal que no sea enfermero
            return personalDb.stream()
                    .filter(p -> p.getIdTipoPersonal() != null && p.getIdTipoPersonal() != 2) // No enfermero
                    .filter(p -> !p.getNombres().contains("CARLOS")) // Evitar explícitamente a Carlos
                    .findFirst().orElse(null);
        }

        // 3er intento: Devolver al menos un médico válido (No enfermeros)
        return personalDb.stream()
                .filter(p -> p.getIdTipoPersonal() != null && p.getIdTipoPersonal() == 1) // Solo médicos
                .findFirst().orElse(null);
    }
}
