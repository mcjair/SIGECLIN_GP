package com.sigeclin.filiacion.repository;

import com.sigeclin.filiacion.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"roles"})
    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE Usuario u SET u.passwordHash = :passwordHash, u.requiereCambioPassword = false, u.fechaCambioPassword = :fecha WHERE u.username = :username")
    void actualizarPassword(
        @org.springframework.data.repository.query.Param("username") String username, 
        @org.springframework.data.repository.query.Param("passwordHash") String passwordHash, 
        @org.springframework.data.repository.query.Param("fecha") java.time.LocalDateTime fecha
    );

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Usuario u SET u.intentosFallidos = 0, u.fechaUltimoAcceso = :fecha WHERE u.username = :username")
    void updateLoginSuccess(@org.springframework.data.repository.query.Param("username") String username, @org.springframework.data.repository.query.Param("fecha") java.time.LocalDateTime fecha);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Usuario u SET u.intentosFallidos = u.intentosFallidos + 1, u.cuentaBloqueada = CASE WHEN (u.intentosFallidos + 1) >= :maxIntentos THEN true ELSE u.cuentaBloqueada END WHERE u.username = :username")
    void updateFailedAttempt(@org.springframework.data.repository.query.Param("username") String username, @org.springframework.data.repository.query.Param("maxIntentos") int maxIntentos);
}
