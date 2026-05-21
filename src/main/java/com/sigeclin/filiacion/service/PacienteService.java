package com.sigeclin.filiacion.service;

import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    @Transactional
    public Paciente registrarPaciente(Paciente paciente) {
        // Buscar si ya existe por Documento para evitar duplicados o actualizar datos
        java.util.Optional<Paciente> existente = pacienteRepository.findByNumeroDocumento(paciente.getNumeroDocumento());
        
        if (existente.isPresent()) {
            Paciente p = existente.get();
            // Actualizamos datos demográficos y el servicio solicitado
            p.setServicioSolicitado(paciente.getServicioSolicitado());
            p.setDireccion(paciente.getDireccion());
            p.setReferenciaDireccion(paciente.getReferenciaDireccion());
            p.setNombres(paciente.getNombres());
            p.setApellidoPaterno(paciente.getApellidoPaterno());
            p.setApellidoMaterno(paciente.getApellidoMaterno());
            p.setSexo(paciente.getSexo());
            p.setFechaNacimiento(paciente.getFechaNacimiento());
            p.setTipoDocumento(paciente.getTipoDocumento());
            
            // IMPORTANTE: Resetear el estado para que aparezca en el flujo de Caja nuevamente
            p.setEstado("PENDIENTE_PAGO");
            p.setFechaCreacion(LocalDateTime.now()); // Actualizar fecha para orden de llegada
            
            System.out.println("Actualizando paciente recurrente: " + p.getNumeroDocumento() + " - Estado: PENDIENTE_PAGO - Nueva fecha: " + p.getFechaCreacion());
            return pacienteRepository.save(p);
        }

        // 1. Generar número de Historia Clínica (AÑO-CORRELATIVO)
        String correlativo = String.format("%06d", pacienteRepository.count() + 1);
        String numeroHC = LocalDate.now().getYear() + "-" + correlativo;
        paciente.setNumeroHistoriaClinica(numeroHC);
        
        // 2. Metadatos
        paciente.setFechaCreacion(LocalDateTime.now());
        paciente.setFechaRegistro(LocalDateTime.now());
        paciente.setEstado("PENDIENTE_PAGO");

        System.out.println("Guardando nuevo paciente: " + paciente.getNumeroDocumento() + " con HC: " + numeroHC);
        return pacienteRepository.save(paciente);
    }

    public java.util.Optional<Paciente> buscarPorId(Integer id) {
        return pacienteRepository.findById(id);
    }

    public java.util.List<Paciente> obtenerPacientesRecientes() {
        return pacienteRepository.findByEstadoOrderByFechaCreacionAsc("PENDIENTE_PAGO");
    }

    public java.util.List<Paciente> obtenerTodos() {
        return pacienteRepository.findAll();
    }

    public java.util.List<Paciente> obtenerPendientesTriaje() {
        return pacienteRepository.findByEstadoOrderByFechaCreacionAsc("PENDIENTE_TRIAJE");
    }

    public java.util.List<Paciente> obtenerColaConsulta(String servicio) {
        return pacienteRepository.findByEstadoAndServicioSolicitadoOrderByFechaCreacionAsc("PENDIENTE_CONSULTA", servicio);
    }

    @Transactional
    public void actualizarEstado(Integer idPaciente, String nuevoEstado) {
        pacienteRepository.findById(idPaciente).ifPresent(p -> {
            p.setEstado(nuevoEstado);
            pacienteRepository.save(p);
        });
    }

    @Transactional
    public void actualizarEstadoPorDocumento(String doc, String nuevoEstado) {
        buscarPorDniOHC(doc).ifPresentOrElse(p -> {
            p.setEstado(nuevoEstado);
            pacienteRepository.save(p);
            System.out.println("Estado actualizado a " + nuevoEstado + " para paciente: " + doc);
        }, () -> {
            System.err.println("No se pudo actualizar estado. Paciente no encontrado con: " + doc);
        });
    }

    public java.util.Optional<Paciente> buscarPorDniOHC(String query) {
        // Intentar buscar por número de documento o HC
        java.util.Optional<Paciente> p = pacienteRepository.findByNumeroDocumento(query);
        if (p.isEmpty()) {
            p = pacienteRepository.findByNumeroHistoriaClinica(query);
        }
        return p;
    }
}
