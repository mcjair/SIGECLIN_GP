package com.sigeclin.filiacion.repository;

import com.sigeclin.filiacion.model.Persona;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sigeclin.filiacion.model.Personal;

@Repository
public interface PersonalRepository extends JpaRepository<Personal, Integer> {

    Optional<Personal> findByNumeroDocumento(String numeroDocumento);
    
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Personal p, Usuario u WHERE p.idUsuario = u.idPersona AND u.username = :username")
    Optional<Personal> findByUsuarioUsername(@org.springframework.data.repository.query.Param("username") String username);
}
