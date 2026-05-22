package com.sigeclin.filiacion.repository;

import com.sigeclin.filiacion.model.Persona;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sigeclin.filiacion.model.Personal;

@Repository
public interface PersonalRepository extends JpaRepository<Personal, Integer> {

    Optional<Personal> findByNumeroDocumento(String numeroDocumento);
    
    Optional<Personal> findByUsuarioUsername(String username);
}
