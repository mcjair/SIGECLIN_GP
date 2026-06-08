package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.Triaje;
import com.sigeclin.clinico.repository.TriajeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriajeService implements ITriajeService {

    private final TriajeRepository triajeRepository;

    @Transactional
    public Triaje guardarTriaje(Triaje triaje) {
        evaluarAlertasClinicas(triaje);
        return triajeRepository.save(triaje);
    }

    public void evaluarAlertasClinicas(Triaje triaje) {
        StringBuilder alertas = new StringBuilder();
        
        // 1. Presión Arterial (Normal: 90/60 - 120/80)
        if (triaje.getPresionArterialSistolica() != null && triaje.getPresionArterialDiastolica() != null) {
            if (triaje.getPresionArterialSistolica() >= 140 || triaje.getPresionArterialDiastolica() >= 90) {
                alertas.append("⚠️ HIPERTENSIÓN DETECTADA. ");
            } else if (triaje.getPresionArterialSistolica() < 90 || triaje.getPresionArterialDiastolica() < 60) {
                alertas.append("⚠️ HIPOTENSIÓN DETECTADA. ");
            }
        }

        // 2. Frecuencia Cardíaca (Normal: 60 - 100 bpm)
        if (triaje.getFrecuenciaCardiaca() != null) {
            if (triaje.getFrecuenciaCardiaca() > 100) alertas.append("⚠️ TAQUICARDIA. ");
            else if (triaje.getFrecuenciaCardiaca() < 60) alertas.append("⚠️ BRADICARDIA. ");
        }

        // 3. Saturación de Oxígeno (Normal: >= 95%)
        if (triaje.getSaturacionOxigeno() != null && triaje.getSaturacionOxigeno() < 95) {
            alertas.append("⚠️ SATURACIÓN BAJA (HIPOXIA). ");
        }

        // 4. Temperatura (Normal: 36.5 - 37.5)
        if (triaje.getTemperatura() != null) {
            double temp = triaje.getTemperatura().doubleValue();
            if (temp >= 38.0) alertas.append("⚠️ ESTADO FEBRIL. ");
            else if (temp < 35.5) alertas.append("⚠️ HIPOTERMIA. ");
        }

        if (alertas.length() > 0) {
            triaje.setAlertaClinica(true);
            triaje.setDetalleAlerta(alertas.toString().trim());
        } else {
            triaje.setAlertaClinica(false);
            triaje.setDetalleAlerta(null);
        }
    }

    public List<Triaje> obtenerHistorialTriaje(Integer idPaciente) {
        return triajeRepository.findByPacienteIdPersonaOrderByFechaHoraDesc(idPaciente);
    }
}
