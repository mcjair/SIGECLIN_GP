package com.sigeclin.filiacion.service;

import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.repository.PacienteRepository;
import com.sigeclin.filiacion.repository.specification.PacienteSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PacienteService implements IPacienteService {

    private final PacienteRepository pacienteRepository;

    @Transactional
    public Paciente registrarPaciente(Paciente paciente) {
        if (paciente.getTipoDocumento() == null || paciente.getTipoDocumento().getIdTipoDocumento() == null) {
            throw new IllegalArgumentException("El tipo de documento es obligatorio");
        }
        // Buscar si ya existe por Documento y Tipo para evitar duplicados o actualizar datos
        java.util.Optional<Paciente> existente = pacienteRepository.findByTipoDocumento_IdTipoDocumentoAndNumeroDocumento(
                paciente.getTipoDocumento().getIdTipoDocumento(), paciente.getNumeroDocumento());
        
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
            
            log.debug("Actualizando paciente recurrente: {} - HC: {}", p.getNumeroDocumento(), p.getNumeroHistoriaClinica());
            return pacienteRepository.save(p);
        }

        // El número de historia clínica es el número de documento del paciente
        paciente.setNumeroHistoriaClinica(paciente.getNumeroDocumento());
        
        // 2. Metadatos
        paciente.setFechaCreacion(LocalDateTime.now());
        paciente.setFechaRegistro(LocalDateTime.now());
        paciente.setEstado("PENDIENTE_PAGO");

        log.info("Nuevo paciente registrado: {} - HC: {}", paciente.getNumeroDocumento(), paciente.getNumeroHistoriaClinica());
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

    public java.util.List<Paciente> obtenerTodos(String servicioFiltro) {
        return pacienteRepository.findAll(PacienteSpecification.conFiltro(null, servicioFiltro));
    }

    public Page<Paciente> obtenerTodosPaginado(String search, String servicioFiltro, Pageable pageable) {
        return pacienteRepository.findAll(PacienteSpecification.conFiltro(search, servicioFiltro), pageable);
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
            log.debug("Estado actualizado a {} para paciente: {}", nuevoEstado, doc);
        }, () -> {
            log.warn("No se pudo actualizar estado. Paciente no encontrado con: {}", doc);
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
