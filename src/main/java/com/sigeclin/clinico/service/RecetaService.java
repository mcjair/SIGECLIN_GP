package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.AlergiaPaciente;
import com.sigeclin.clinico.model.Consulta;
import com.sigeclin.clinico.model.DetalleReceta;
import com.sigeclin.clinico.model.RecetaMedica;
import com.sigeclin.clinico.repository.AlergiaPacienteRepository;
import com.sigeclin.clinico.repository.DetalleRecetaRepository;
import com.sigeclin.clinico.repository.RecetaRepository;
import com.sigeclin.exception.AlergiaActivaException;
import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.model.Personal;
import com.sigeclin.maestras.model.Medicamento;
import com.sigeclin.maestras.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecetaService implements IRecetaService {

    private final AlergiaPacienteRepository alergiaRepository;
    private final RecetaRepository recetaRepository;
    private final DetalleRecetaRepository detalleRecetaRepository;
    private final MedicamentoRepository medicamentoRepository;

    @Transactional
    public RecetaMedica emitirReceta(RecetaMedica receta, List<DetalleReceta> detalles) {
        Integer idPaciente = receta.getPaciente().getIdPersona();

        List<AlergiaPaciente> alergias = alergiaRepository.findByPacienteIdPersonaAndActivaTrue(idPaciente);

        if (!alergias.isEmpty()) {
            for (DetalleReceta detalle : detalles) {
                Medicamento medicamento = detalle.getMedicamento();
                if (medicamento == null) continue;

                for (AlergiaPaciente alergia : alergias) {
                    if (alergia.getMedicamento() != null &&
                        alergia.getMedicamento().getIdMedicamento().equals(medicamento.getIdMedicamento())) {
                        log.warn("ALERTA: El paciente {} tiene alergia activa a {}",
                                idPaciente, medicamento.getNombreGenerico());
                        throw new AlergiaActivaException(
                            String.format("ALERTA: El paciente tiene alergia registrada a %s. " +
                                    "Se requiere confirmación explícita para prescribir.",
                                    medicamento.getNombreGenerico())
                        );
                    }
                }
            }
        }

        receta.setEstado("emitida");
        RecetaMedica saved = recetaRepository.save(receta);

        for (DetalleReceta detalle : detalles) {
            detalle.setReceta(saved);
            detalleRecetaRepository.save(detalle);
        }

        log.info("Receta {} emitida para paciente {} con {} ítem(s)",
                saved.getIdReceta(), idPaciente, detalles.size());
        return saved;
    }

    @Transactional
    public void emitirReceta(Consulta consulta, Paciente paciente, Personal medico,
                              String planTratamiento, List<Map<String, Object>> medicamentos) {
        if (medicamentos == null || medicamentos.isEmpty()) return;

        RecetaMedica receta = new RecetaMedica();
        receta.setConsulta(consulta);
        receta.setPaciente(paciente);
        receta.setMedico(medico);
        receta.setIndicacionesGenerales(planTratamiento);
        receta.setFechaEmision(LocalDateTime.now());
        receta.setEstado("emitida");

        List<DetalleReceta> detalles = new ArrayList<>();

        for (Map<String, Object> medData : medicamentos) {
            DetalleReceta detalle = new DetalleReceta();
            detalle.setReceta(receta);

            Medicamento medicamento = null;
            if (medData.get("id") != null && !medData.get("id").toString().equals("1")) {
                try {
                    medicamento = medicamentoRepository.findById(
                            Integer.parseInt(medData.get("id").toString())).orElse(null);
                } catch (Exception e) {
                    log.debug("Error al buscar medicamento por ID: {}", e.getMessage());
                }
            }

            if (medicamento == null && medData.get("nombre") != null) {
                List<Medicamento> found = medicamentoRepository.buscarPorNombre(
                        (String) medData.get("nombre"));
                if (!found.isEmpty()) {
                    medicamento = found.get(0);
                }
            }

            if (medicamento == null) {
                medicamento = medicamentoRepository.findAll().stream().findFirst().orElse(null);
            }

            if (medicamento == null) continue;

            detalle.setMedicamento(medicamento);
            detalle.setDosis((String) medData.get("dosis"));
            detalle.setFrecuencia((String) medData.get("frecuencia"));

            int duracion = 1;
            if (medData.get("duracion") != null) {
                try {
                    String durStr = medData.get("duracion").toString().replaceAll("[^0-9]", "");
                    if (!durStr.isEmpty()) {
                        duracion = Integer.parseInt(durStr);
                    }
                } catch (Exception e) {
                    duracion = 1;
                }
            }
            detalle.setDuracionDias(duracion);

            Object cantObj = medData.get("cantidad");
            detalle.setCantidadTotal(cantObj != null ? Integer.parseInt(cantObj.toString()) : 1);
            detalle.setIdViaAdministracion(1);
            detalle.setEstadoDispensacion("pendiente");

            detalles.add(detalle);
        }

        emitirReceta(receta, detalles);
    }
}
