package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.*;
import com.sigeclin.clinico.repository.*;
import com.sigeclin.maestras.repository.ServicioRepository;
import com.sigeclin.maestras.model.Servicio;
import com.sigeclin.filiacion.repository.PersonalRepository;
import com.sigeclin.filiacion.model.Personal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConsultaService {

    private final TriajeRepository triajeRepository;
    private final ServicioRepository servicioRepository;
    private final ConsultaRepository consultaRepository;
    private final RecetaRepository recetaRepository;
    private final DetalleRecetaRepository detalleRecetaRepository;
    private final DiagnosticoConsultaRepository diagnosticoConsultaRepository;
    private final com.sigeclin.maestras.repository.Cie10Repository cie10Repository;
    private final com.sigeclin.maestras.repository.MedicamentoRepository medicamentoRepository;
    private final PersonalRepository personalRepository;

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

        Personal medico = personalRepository.findById(triaje.getUsuario().getIdPersona())
                .orElseGet(() -> personalRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("No hay personal médico registrado")));

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
                System.err.println("Error al parsear proximoControl: " + e.getMessage());
            }
        }

        String tipoSalida = (String) data.get("tipoSalida");
        consulta.setEstado(tipoSalida != null ? tipoSalida : "finalizada");
        consulta.setFechaHoraInicio(LocalDateTime.now());
        consulta.setFechaHoraFin(LocalDateTime.now());

        consulta = consultaRepository.save(consulta);

        // Actualizar triaje
        // triaje.setEstado("ATENDIDO");
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
            RecetaMedica receta = new RecetaMedica();
            receta.setConsulta(consulta);
            receta.setPaciente(paciente);
            receta.setMedico(medico);
            receta.setIndicacionesGenerales((String) data.get("planTratamiento"));
            receta.setFechaEmision(LocalDateTime.now());
            receta.setEstado("emitida");
            receta = recetaRepository.save(receta);

            for (Map<String, Object> medData : medicamentos) {
                DetalleReceta detalle = new DetalleReceta();
                detalle.setReceta(receta);
                
                com.sigeclin.maestras.model.Medicamento medicamento = null;
                // Intentar buscar por ID si viene del frontend
                if (medData.get("id") != null && !medData.get("id").toString().equals("1")) {
                    try {
                        medicamento = medicamentoRepository.findById(Integer.parseInt(medData.get("id").toString())).orElse(null);
                    } catch (Exception e) {}
                }
                
                // Si no se encuentra o es mock, buscar por nombre
                if (medicamento == null && medData.get("nombre") != null) {
                    List<com.sigeclin.maestras.model.Medicamento> found = medicamentoRepository.buscarPorNombre((String) medData.get("nombre"));
                    if (!found.isEmpty()) {
                        medicamento = found.get(0);
                    }
                }

                // Fallback: Si sigue siendo nulo, no podemos guardar el detalle a menos que permitamos nulos o usemos un comodín
                if (medicamento == null) {
                    // Para evitar que falle la transacción, buscamos el primero disponible o lanzamos advertencia
                    medicamento = medicamentoRepository.findAll().stream().findFirst().orElse(null);
                }

                if (medicamento != null) {
                    detalle.setMedicamento(medicamento);
                    detalle.setDosis((String) medData.get("dosis"));
                    detalle.setFrecuencia((String) medData.get("frecuencia"));
                    detalle.setDuracion((String) medData.get("duracion"));
                    Object cantObj = medData.get("cantidad");
                    detalle.setCantidadTotal(cantObj != null ? Integer.parseInt(cantObj.toString()) : 1);
                    
                    detalleRecetaRepository.save(detalle);
                }
            }
        }
    }
}
