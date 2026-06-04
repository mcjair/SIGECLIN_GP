package com.sigeclin.filiacion.service;

import com.sigeclin.filiacion.model.Personal;
import com.sigeclin.filiacion.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalService implements IPersonalService {

    private final PersonalRepository personalRepository;
    private final jakarta.persistence.EntityManager entityManager;

    public List<Personal> listarTodos() {
        return personalRepository.findAll();
    }

    public Personal buscarPorId(Integer id) {
        Validate.notNull(id, "El ID del personal no puede ser nulo");
        return personalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personal no encontrado con ID: " + id));
    }

    @Transactional
    public Personal guardar(Personal personal) {
        Validate.notNull(personal, "Los datos del personal no pueden ser nulos");
        
        if (StringUtils.isNotBlank(personal.getNumeroColegiatura())) {
            personal.setNumeroColegiatura(StringUtils.upperCase(personal.getNumeroColegiatura()).replace(" ", "-"));
        }
        if (personal.getNombres() != null) {
            personal.setNombres(StringUtils.trim(personal.getNombres()));
        }
        if (personal.getApellidoPaterno() != null) {
            personal.setApellidoPaterno(StringUtils.trim(personal.getApellidoPaterno()));
        }

        if (personal.getIdPersona() != null) {
            // Edición de personal existente: cargar entidad original para conservar campos no presentes en el formulario
            Personal existente = buscarPorId(personal.getIdPersona());
            
            // Copiar datos de Persona
            existente.setTipoDocumento(personal.getTipoDocumento());
            existente.setNumeroDocumento(personal.getNumeroDocumento());
            existente.setNombres(personal.getNombres());
            existente.setApellidoPaterno(personal.getApellidoPaterno());
            existente.setApellidoMaterno(personal.getApellidoMaterno());
            existente.setSexo(personal.getSexo());
            existente.setFechaNacimiento(personal.getFechaNacimiento());
            existente.setTelefonoPrincipal(personal.getTelefonoPrincipal());
            existente.setCorreoElectronico(personal.getCorreoElectronico());
            
            // Copiar datos de Personal
            existente.setIdTipoPersonal(personal.getIdTipoPersonal());
            existente.setIdEspecialidad(personal.getIdEspecialidad());
            existente.setNumeroColegiatura(personal.getNumeroColegiatura());
            existente.setFechaIngreso(personal.getFechaIngreso());
            
            existente.setFechaActualizacion(LocalDateTime.now());
            return personalRepository.save(existente);
        } else {
            // Check if Persona already exists (e.g. as a Paciente)
            try {
                List<?> existing = entityManager.createNativeQuery("SELECT id_persona FROM filiacion.persona WHERE numero_documento = :num AND id_tipo_documento = :tipo")
                        .setParameter("num", personal.getNumeroDocumento())
                        .setParameter("tipo", personal.getTipoDocumento().getIdTipoDocumento())
                        .getResultList();
                        
                if (!existing.isEmpty()) {
                    Integer existingId = ((Number) existing.get(0)).intValue();
                    
                    // Update persona fields
                    entityManager.createNativeQuery("UPDATE filiacion.persona SET nombres = :nom, apellido_paterno = :ap, apellido_materno = :am, sexo = :sex, fecha_nacimiento = :fn, telefono_principal = :tel, correo_electronico = :correo, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id_persona = :id")
                            .setParameter("nom", personal.getNombres())
                            .setParameter("ap", personal.getApellidoPaterno())
                            .setParameter("am", personal.getApellidoMaterno())
                            .setParameter("sex", personal.getSexo())
                            .setParameter("fn", personal.getFechaNacimiento())
                            .setParameter("tel", personal.getTelefonoPrincipal())
                            .setParameter("correo", personal.getCorreoElectronico())
                            .setParameter("id", existingId)
                            .executeUpdate();
                            
                    // Check if they are already in Personal table
                    List<?> existingPersonal = entityManager.createNativeQuery("SELECT id_personal FROM filiacion.personal WHERE id_personal = :id")
                            .setParameter("id", existingId)
                            .getResultList();
                            
                    if (existingPersonal.isEmpty()) {
                        // Insert into personal
                        entityManager.createNativeQuery("INSERT INTO filiacion.personal (id_personal, id_tipo_personal, id_especialidad, numero_colegiatura, fecha_ingreso, estado_laboral, fecha_creacion) VALUES (:id, :tipo, :esp, :col, :ingreso, :estado, CURRENT_TIMESTAMP)")
                                .setParameter("id", existingId)
                                .setParameter("tipo", personal.getIdTipoPersonal())
                                .setParameter("esp", personal.getIdEspecialidad())
                                .setParameter("col", personal.getNumeroColegiatura())
                                .setParameter("ingreso", personal.getFechaIngreso())
                                .setParameter("estado", "activo")
                                .executeUpdate();
                    }
                    
                    return buscarPorId(existingId);
                }
            } catch (Exception e) {
                log.error("Error linking existing persona to personal: {}", e.getMessage(), e);
            }
            
            // Registro de nuevo personal normal
            personal.setFechaCreacion(LocalDateTime.now());
            personal.setFechaActualizacion(LocalDateTime.now());
            return personalRepository.save(personal);
        }
    }

    @Transactional
    public void eliminar(Integer id) {
        Personal p = buscarPorId(id);
        p.setEstadoLaboral("inactivo");
        personalRepository.save(p);
    }

    @Transactional
    public void toggleEstado(Integer id) {
        Personal p = buscarPorId(id);
        p.setEstadoLaboral("activo".equals(p.getEstadoLaboral()) ? "inactivo" : "activo");
        personalRepository.save(p);
    }
}
