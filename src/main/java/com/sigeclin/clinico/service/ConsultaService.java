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
    private final IRecetaService recetaService;

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
    public void guardarConsultaCompleta(Integer triajeId, Map<String, Object> data) {
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
        if (medico == null) {
            medico = personalRepository.findById(triaje.getUsuario().getIdPersona())
                    .orElseGet(() -> personalRepository.findAll().stream().findFirst()
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
    }
}
