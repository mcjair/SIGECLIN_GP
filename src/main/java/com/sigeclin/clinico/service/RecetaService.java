package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.Consulta;
import com.sigeclin.clinico.repository.AlergiaPacienteRepository;
import com.sigeclin.clinico.repository.RecetaRepository;
import com.sigeclin.exception.AlergiaActivaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecetaService {

    private final AlergiaPacienteRepository alergiaRepository;
    private final RecetaRepository recetaRepository;
    // private final MedicamentoRepository medicamentoRepository;

    /*
    @Transactional
    public RecetaMedica emitirReceta(RecetaRequestDTO dto) {
        // 1. Obtener alergias activas del paciente
        List<AlergiaPaciente> alergias = alergiaRepository
            .findByIdPacienteAndActivaTrue(dto.getIdPaciente());

        // 2. Para cada detalle de receta, validar contra alergias
        for (DetalleRecetaDTO detalle : dto.getDetalles()) {
            CatalogoMedicamentos medicamento = medicamentoRepository
                .findById(detalle.getIdMedicamento())
                .orElseThrow(() -> new MedicamentoNotFoundException());

            for (AlergiaPaciente alergia : alergias) {
                // Validar por familia farmacológica
                if (alergia.getIdFamilia() != null &&
                    alergia.getIdFamilia().equals(medicamento.getIdFamilia())) {
                    throw new AlergiaActivaException(
                        String.format("ALERTA: El paciente tiene alergia a la familia %s. " +
                            "Medicamento prescrito: %s. Se requiere confirmación explícita.",
                            alergia.getFamiliaFarmacologica().getDescripcion(),
                            medicamento.getNombreGenerico())
                    );
                }
                // Validar por medicamento específico
                if (alergia.getIdMedicamento() != null &&
                    alergia.getIdMedicamento().equals(medicamento.getIdMedicamento())) {
                    throw new AlergiaActivaException(
                        String.format("ALERTA: El paciente tiene alergia a %s.",
                            medicamento.getNombreGenerico())
                    );
                }
            }
        }
        // 3. Si pasa validación, emitir receta
        return recetaRepository.save(nuevaReceta);
    }
    */
}
