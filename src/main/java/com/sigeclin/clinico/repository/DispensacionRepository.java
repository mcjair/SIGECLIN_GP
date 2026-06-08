package com.sigeclin.clinico.repository;

import com.sigeclin.clinico.model.Dispensacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DispensacionRepository extends JpaRepository<Dispensacion, Integer> {
    List<Dispensacion> findByDetalleRecetaIdDetalleReceta(Integer idDetalleReceta);
    List<Dispensacion> findByLoteMedicamentoIdMedicamentoOrderByFechaDispensacionDesc(Integer idMedicamento);
    List<Dispensacion> findAllByOrderByFechaDispensacionDesc();
    @Query("SELECT d FROM Dispensacion d JOIN FETCH d.detalleReceta dr JOIN FETCH dr.medicamento JOIN FETCH dr.receta r JOIN FETCH r.paciente JOIN FETCH d.lote JOIN FETCH d.usuario ORDER BY d.fechaDispensacion DESC")
    List<Dispensacion> findAllWithDetails();
}
