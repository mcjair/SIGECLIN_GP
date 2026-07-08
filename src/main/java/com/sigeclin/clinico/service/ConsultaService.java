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

    private static final String ATTR_TIENE_CERTIFICADO = "tieneCertificado";
    private static final String ATTR_PROXIMO_CONTROL = "proximoControl";
    private static final String ATTR_MOTIVO = "motivo";

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

    public List<Consulta> obtenerHistorialPaciente(Integer idPaciente) {
        // Fallback estructurado: Limitar a 50 para evitar OutOfMemory y usar EntityGraph contra N+1
        return consultaRepository.findByPacienteIdPersonaOrderByFechaHoraInicioDesc(idPaciente, org.springframework.data.domain.PageRequest.of(0, 50)).getContent();
    }

    private List<String> extraerMedicamentosDeConsulta(Consulta c) {
        List<String> medicamentos = new java.util.ArrayList<>();
        if (c.getRecetas() == null) {
            return medicamentos;
        }
        for (var r : c.getRecetas()) {
            if (r.getDetalles() == null) {
                continue;
            }
            for (var d : r.getDetalles()) {
                if (d.getMedicamento() != null) {
                    medicamentos.add(d.getMedicamento().getNombreGenerico());
                }
            }
        }
        return medicamentos;
    }

    private String extraerDiagnosticoDeConsulta(Consulta c) {
        if (c.getDiagnosticos() == null || c.getDiagnosticos().isEmpty()) {
            return "POR DEFINIR";
        }
        return c.getDiagnosticos().stream()
                .map(d -> d.getCie10().getCodigo() + " - " + d.getCie10().getDescripcion())
                .collect(java.util.stream.Collectors.joining("; "));
    }

    private void extraerSignosVitalesDeConsulta(Consulta c, Map<String, Object> map) {
        if (c.getTriaje() != null) {
            var t = c.getTriaje();
            String sistolica = t.getPresionArterialSistolica() != null ? t.getPresionArterialSistolica().toString() : "--";
            String diastolica = t.getPresionArterialDiastolica() != null ? t.getPresionArterialDiastolica().toString() : "--";
            map.put("pa", sistolica + "/" + diastolica);
            map.put("temp", t.getTemperatura() != null ? t.getTemperatura().toString() : "--");
            map.put("fc", t.getFrecuenciaCardiaca() != null ? t.getFrecuenciaCardiaca().toString() : "--");
            map.put("sat", t.getSaturacionOxigeno() != null ? t.getSaturacionOxigeno().toString() : "--");
        } else {
            map.put("pa", "--/--");
            map.put("temp", "--");
            map.put("fc", "--");
            map.put("sat", "--");
        }
    }

    private Map<String, Object> mapearConsultaADto(Consulta c, java.time.format.DateTimeFormatter formatter) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("fecha", c.getFechaHoraInicio() != null ? c.getFechaHoraInicio().format(formatter) : "S/F");
        
        String servicio = c.getTriaje() != null && c.getTriaje().getServicioDestino() != null ? c.getTriaje().getServicioDestino() : "MÉDICO GENERAL";
        map.put("servicio", servicio);
        
        String medico = c.getMedico() != null ? c.getMedico().getNombres() + " " + c.getMedico().getApellidoPaterno() : "Asignado";
        map.put("medico", "Dr(a). " + medico);
        
        map.put(ATTR_MOTIVO, c.getMotivoConsulta() != null ? c.getMotivoConsulta() : "Sin descripción");
        map.put("anamnesis", c.getAnamnesis() != null ? c.getAnamnesis() : "---");
        map.put("examen", c.getExamenFisico() != null ? c.getExamenFisico() : "---");
        map.put("plan", c.getPlanTratamiento() != null ? c.getPlanTratamiento() : "---");
        map.put("idAtencion", c.getIdConsulta());
        map.put(ATTR_PROXIMO_CONTROL, c.getProximoControl() != null ? c.getProximoControl().toString() : "---");
        
        map.put("medicamentos", extraerMedicamentosDeConsulta(c));
        map.put(ATTR_TIENE_CERTIFICADO, c.getTieneCertificado() != null && c.getTieneCertificado());
        map.put("estadoSalida", c.getEstado());
        map.put("diagnostico", extraerDiagnosticoDeConsulta(c));
        extraerSignosVitalesDeConsulta(c, map);
        
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerHistorialPacienteDto(Integer idPaciente, String finalRolFiltro) {
        List<Consulta> historial = consultaRepository.findByPacienteIdPersonaOrderByFechaHoraInicioDesc(idPaciente, org.springframework.data.domain.PageRequest.of(0, 50)).getContent();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        return historial.stream()
            .filter(c -> {
                if (finalRolFiltro == null) return true;
                String servicio = c.getTriaje() != null && c.getTriaje().getServicioDestino() != null 
                        ? c.getTriaje().getServicioDestino() 
                        : "MÉDICO GENERAL";
                return servicio.equalsIgnoreCase(finalRolFiltro);
            })
            .limit(7)
            .map(c -> mapearConsultaADto(c, formatter))
            .toList();
    }

    public List<Triaje> obtenerPacientesEnEspera() {
        LocalDateTime start = LocalDateTime.of(java.time.LocalDate.now(java.time.ZoneId.systemDefault()), java.time.LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(java.time.LocalDate.now(java.time.ZoneId.systemDefault()), java.time.LocalTime.MAX);
        return triajeRepository.findByFechaHoraBetweenOrderByFechaHoraAsc(start, end);
    }

    public List<Triaje> obtenerPacientesEnEsperaPorModulo(String modulo) {
        LocalDateTime start = LocalDateTime.of(java.time.LocalDate.now(java.time.ZoneId.systemDefault()), java.time.LocalTime.MIN);
        return triajeRepository.buscarPendientesPorModulo(modulo, start);
    }

    public Triaje obtenerTriajePorId(Integer id) {
        return triajeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Triaje no encontrado"));
    }

    public List<Servicio> obtenerServiciosActivos() {
        return servicioRepository.findByActivoTrue();
    }

    public java.util.Optional<Triaje> buscarUltimoTriajePorDocumento(String doc) {
        return triajeRepository.findTopByPacienteNumeroDocumentoOrPacienteNumeroHistoriaClinicaOrderByFechaHoraDesc(doc, doc);
    }

    private Personal resolverMedicoDeConsulta(Triaje triaje, String username) {
        Personal medico = null;
        if (username != null) {
            medico = personalRepository.findByUsuarioUsername(username).orElse(null);
        }
        if (medico == null && triaje.getUsuario() != null) {
            medico = personalRepository.findByUsuarioUsername(triaje.getUsuario().getUsername()).orElse(null);
        }
        if (medico == null) {
            Integer especialidadBuscada = 1;
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
                            .orElseThrow(() -> new IllegalArgumentException("No hay personal médico registrado")));
        }
        return medico;
    }

    private void guardarDiagnosticosDeConsulta(Consulta consulta, List<Map<String, Object>> diagnosticos) {
        if (diagnosticos == null) return;
        for (Map<String, Object> diagData : diagnosticos) {
            String codigo = (String) diagData.get("codigo");
            if (codigo == null || codigo.isEmpty()) continue;

            DiagnosticoConsulta diag = new DiagnosticoConsulta();
            diag.setConsulta(consulta);
            
            com.sigeclin.maestras.model.Cie10 cie10 = cie10Repository.findByCodigo(codigo).orElse(null);
            if (cie10 == null) {
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

    private void registrarOrdenLaboratorio(Consulta consulta, Personal medico, List<Map<String, Object>> examenes) {
        if (examenes == null || examenes.isEmpty()) return;
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
        orden.setIndicaciones(consulta.getPlanTratamiento());

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

    private String obtenerUsernameActual() {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                return auth.getName();
            }
        } catch (Exception e) {
            // Ignorar
        }
        return null;
    }

    private java.time.LocalDate parsearProximoControl(Object value) {
        if (value != null && !value.toString().isEmpty()) {
            try {
                return java.time.LocalDate.parse(value.toString());
            } catch (Exception e) {
                log.warn("Error al parsear proximoControl: {}", e.getMessage());
            }
        }
        return null;
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Consulta guardarConsultaCompleta(Integer triajeId, Map<String, Object> data) {
        if (consultaRepository.existsByTriajeIdTriaje(triajeId)) {
            throw new IllegalArgumentException("Este paciente ya fue atendido para el triaje indicado. No se puede duplicar ni sobrescribir la atención.");
        }
        
        Triaje triaje = triajeRepository.findById(triajeId)
                .orElseThrow(() -> new IllegalArgumentException("Triaje no encontrado"));

        com.sigeclin.filiacion.model.Paciente paciente = triaje.getPaciente();

        String username = obtenerUsernameActual();

        Personal medico = resolverMedicoDeConsulta(triaje, username);

        Consulta consulta = new Consulta();
        consulta.setPaciente(paciente);
        consulta.setTriaje(triaje);
        consulta.setMedico(medico);
        consulta.setIdEspecialidad(medico.getIdEspecialidad() != null ? medico.getIdEspecialidad() : 1);
        String motivo = data.get(ATTR_MOTIVO) != null ? (String) data.get(ATTR_MOTIVO) : "Atención Médica General";
        consulta.setMotivoConsulta(motivo.toUpperCase());
        
        String anamnesis = (String) data.get("anamnesis");
        consulta.setAnamnesis(anamnesis != null ? anamnesis.toUpperCase() : null);
        
        String examenFisico = (String) data.get("examenFisico");
        consulta.setExamenFisico(examenFisico != null ? examenFisico.toUpperCase() : null);
        
        String planTrat = (String) data.get("planTratamiento");
        consulta.setPlanTratamiento(planTrat != null ? planTrat.toUpperCase() : null);
        
        consulta.setProximoControl(parsearProximoControl(data.get(ATTR_PROXIMO_CONTROL)));

        String tipoSalida = (String) data.get("tipoSalida");
        consulta.setEstado(tipoSalida != null ? tipoSalida : "finalizada");
        
        Boolean tieneCert = data.get(ATTR_TIENE_CERTIFICADO) != null && (Boolean) data.get(ATTR_TIENE_CERTIFICADO);
        consulta.setTieneCertificado(tieneCert);
        
        String obsCert = (String) data.get("observacionesCertificado");
        consulta.setObservacionesCertificado(obsCert != null ? obsCert.toUpperCase() : null);

        consulta.setFechaHoraInicio(LocalDateTime.now(java.time.ZoneId.systemDefault()));
        consulta.setFechaHoraFin(LocalDateTime.now(java.time.ZoneId.systemDefault()));

        consulta = consultaRepository.save(consulta);

        triajeRepository.save(triaje);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> diagnosticos = (List<Map<String, Object>>) data.get("diagnosticos");
        guardarDiagnosticosDeConsulta(consulta, diagnosticos);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> medicamentos = (List<Map<String, Object>>) data.get("medicamentos");
        if (medicamentos != null && !medicamentos.isEmpty()) {
            recetaService.emitirReceta(consulta, paciente, medico,
                    (String) data.get("planTratamiento"), medicamentos);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> examenes = (List<Map<String, Object>>) data.get("examenes");
        registrarOrdenLaboratorio(consulta, medico, examenes);

        return consulta;
    }
}
