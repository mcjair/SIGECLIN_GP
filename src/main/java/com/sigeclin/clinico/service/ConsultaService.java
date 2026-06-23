package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.*;
import com.sigeclin.clinico.repository.*;
import com.sigeclin.maestras.repository.ServicioRepository;
import com.sigeclin.maestras.model.Servicio;
import com.sigeclin.filiacion.repository.PersonalRepository;
import com.sigeclin.filiacion.model.Personal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultaService implements IConsultaService {

    private final TriajeRepository triajeRepository;
    private final ServicioRepository servicioRepository;
    private final ConsultaRepository consultaRepository;
    private final DiagnosticoConsultaRepository diagnosticoConsultaRepository;
    private final com.sigeclin.maestras.repository.Cie10Repository cie10Repository;
    private final PersonalRepository personalRepository;
    private final com.sigeclin.filiacion.repository.UsuarioRepository usuarioRepository;
    private final IRecetaService recetaService;
    private final OrdenMedicaRepository ordenMedicaRepository;
    private final ResultadoLaboratorioRepository resultadoLaboratorioRepository;
    private final JdbcTemplate jdbcTemplate;

    public List<com.sigeclin.clinico.model.Consulta> obtenerHistorialPaciente(Integer idPaciente) {
        return consultaRepository.findByPacienteIdPersonaOrderByFechaHoraInicioDesc(idPaciente);
    }

    public List<Triaje> obtenerPacientesEnEspera() {
        LocalDateTime start = LocalDateTime.of(java.time.LocalDate.now(), java.time.LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(java.time.LocalDate.now(), java.time.LocalTime.MAX);
        return triajeRepository.findByFechaHoraBetweenOrderByFechaHoraAsc(start, end);
    }

    public List<Triaje> obtenerPacientesEnEsperaPorModulo(String modulo) {
        LocalDateTime start = LocalDateTime.of(java.time.LocalDate.now(), java.time.LocalTime.MIN);
        return triajeRepository.buscarPendientesPorModulo(modulo, start);
    }

    public Triaje obtenerTriajePorId(Integer id) {
        return triajeRepository.findById(id).orElseThrow(() -> new RuntimeException("Triaje no encontrado"));
    }

    public List<Servicio> obtenerServiciosActivos() {
        return servicioRepository.findByActivoTrue();
    }

    public java.util.Optional<Triaje> buscarUltimoTriajePorDocumento(String doc) {
        return triajeRepository.findTopByPacienteNumeroDocumentoOrPacienteNumeroHistoriaClinicaOrderByFechaHoraDesc(doc, doc);
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Consulta guardarConsultaCompleta(Integer triajeId, Map<String, Object> data) {
        Triaje triaje = triajeRepository.findById(triajeId)
                .orElseThrow(() -> new RuntimeException("Triaje no encontrado"));

        com.sigeclin.filiacion.model.Paciente paciente = triaje.getPaciente();

        String username = null;
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                username = auth.getName();
            }
        } catch (Exception e) {
            // Ignorar si no está en contexto web (ej. tests)
        }

        Personal medico = null;
        if (username != null) {
            medico = personalRepository.findByUsuarioUsername(username).orElse(null);
        }
        if (medico == null && triaje.getUsuario() != null) {
            medico = personalRepository.findByUsuarioUsername(triaje.getUsuario().getUsername()).orElse(null);
        }
        if (medico == null) {
            // Fallback Inteligente para Administradores: Asignar al especialista según el servicio de destino.
            Integer especialidadBuscada = 1; // Default: Medicina General
            if (triaje.getServicioDestino() != null) {
                String dest = triaje.getServicioDestino().toUpperCase();
                if (dest.contains("OBSTETRICIA")) especialidadBuscada = 2;
                else if (dest.contains("ODONTOLOG")) especialidadBuscada = 3;
                else if (dest.contains("PSICOLOG")) especialidadBuscada = 4;
                else if (dest.contains("NUTRICI")) especialidadBuscada = 5;
            }
            final Integer espFinal = especialidadBuscada;

            medico = personalRepository.findAll().stream()
                    .filter(p -> p.getIdEspecialidad() != null && p.getIdEspecialidad().equals(espFinal))
                    .filter(p -> "activo".equalsIgnoreCase(p.getEstadoLaboral()))
                    .findFirst()
                    .orElseGet(() -> personalRepository.findAll().stream()
                            .filter(p -> p.getIdTipoPersonal() != null && p.getIdTipoPersonal() == 1)
                            .filter(p -> "activo".equalsIgnoreCase(p.getEstadoLaboral()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("No hay personal médico registrado")));
        }

        Consulta consulta = new Consulta();
        consulta.setPaciente(paciente);
        consulta.setTriaje(triaje);
        consulta.setMedico(medico);
        consulta.setIdEspecialidad(medico.getIdEspecialidad() != null ? medico.getIdEspecialidad() : 1);
        consulta.setMotivoConsulta(data.get("motivo") != null ? (String) data.get("motivo") : "Atención Médica General");
        consulta.setAnamnesis((String) data.get("anamnesis"));
        consulta.setExamenFisico((String) data.get("examenFisico"));
        consulta.setPlanTratamiento((String) data.get("planTratamiento"));
        
        if (data.get("proximoControl") != null && !data.get("proximoControl").toString().isEmpty()) {
            try {
                consulta.setProximoControl(java.time.LocalDate.parse(data.get("proximoControl").toString()));
            } catch (Exception e) {
                log.warn("Error al parsear proximoControl: {}", e.getMessage());
            }
        }

        String tipoSalida = (String) data.get("tipoSalida");
        consulta.setEstado(tipoSalida != null ? tipoSalida : "finalizada");
        consulta.setFechaHoraInicio(LocalDateTime.now());
        consulta.setFechaHoraFin(LocalDateTime.now());

        consulta = consultaRepository.save(consulta);

        // Actualizar estado del triaje
        triajeRepository.save(triaje);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> diagnosticos = (List<Map<String, Object>>) data.get("diagnosticos");
        if (diagnosticos != null) {
            for (Map<String, Object> diagData : diagnosticos) {
                String codigo = (String) diagData.get("codigo");
                if (codigo == null || codigo.isEmpty()) continue;

                DiagnosticoConsulta diag = new DiagnosticoConsulta();
                diag.setConsulta(consulta);
                
                // Buscar CIE-10 en BD, si no existe se podría crear uno básico o simplemente usar el código si el modelo lo permite
                // Dado que el modelo tiene una relación ManyToOne, debemos buscar el objeto
                com.sigeclin.maestras.model.Cie10 cie10 = cie10Repository.findByCodigo(codigo).orElse(null);
                
                if (cie10 == null) {
                    // Si no existe en BD, lo creamos para mantener integridad referencial
                    cie10 = new com.sigeclin.maestras.model.Cie10();
                    cie10.setCodigo(codigo);
                    cie10.setDescripcion((String) diagData.get("descripcion"));
                    cie10 = cie10Repository.save(cie10);
                }
                
                diag.setCie10(cie10);
                diag.setTipoDiagnostico("DEFINITIVO");
                diagnosticoConsultaRepository.save(diag);
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> medicamentos = (List<Map<String, Object>>) data.get("medicamentos");
        if (medicamentos != null && !medicamentos.isEmpty()) {
            recetaService.emitirReceta(consulta, paciente, medico,
                    (String) data.get("planTratamiento"), medicamentos);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> examenes = (List<Map<String, Object>>) data.get("examenes");
        if (examenes != null && !examenes.isEmpty()) {
            Integer idCiex;
            try {
                idCiex = jdbcTemplate.queryForObject(
                    "SELECT id_ciex FROM maestras.catalogo_ciex WHERE codigo = 'LAB-ORD'", Integer.class);
            } catch (Exception e) {
                log.warn("No se encontró catalogo_ciex para LAB-ORD, se crea automáticamente");
                jdbcTemplate.execute(
                    "INSERT INTO maestras.catalogo_ciex (codigo, descripcion, tipo, activo) " +
                    "VALUES ('LAB-ORD', 'ORDEN DE LABORATORIO', 'LABORATORIO', true) " +
                    "ON CONFLICT (codigo) DO UPDATE SET activo = true");
                idCiex = jdbcTemplate.queryForObject(
                    "SELECT id_ciex FROM maestras.catalogo_ciex WHERE codigo = 'LAB-ORD'", Integer.class);
            }

            OrdenMedica orden = new OrdenMedica();
            orden.setIdConsulta(consulta.getIdConsulta());
            orden.setIdCiex(idCiex);
            orden.setIdPersonalSolicitante(medico.getIdPersona());
            orden.setTipo("LABORATORIO");
            orden.setEstado("solicitada");
            orden.setIndicaciones((String) data.get("planTratamiento"));

            for (Map<String, Object> examData : examenes) {
                ResultadoLaboratorio rl = new ResultadoLaboratorio();
                rl.setCodigoExamen((String) examData.get("codigo"));
                rl.setOrden(orden);
                orden.getResultados().add(rl);
            }

            ordenMedicaRepository.save(orden);
            log.info("Orden de laboratorio creada: idOrden={}, examenes={}",
                orden.getIdOrden(), examenes.size());
        }
        return consulta;
    }
}
