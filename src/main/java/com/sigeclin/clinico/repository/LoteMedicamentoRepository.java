package com.sigeclin.clinico.repository;

import com.sigeclin.clinico.model.LoteMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoteMedicamentoRepository extends JpaRepository<LoteMedicamento, Integer> {
    List<LoteMedicamento> findByMedicamentoIdMedicamentoOrderByFechaVencimientoAsc(Integer idMedicamento);
    List<LoteMedicamento> findByStockActualLessThan(Integer minimo);
    List<LoteMedicamento> findByFechaVencimientoBefore(LocalDate fecha);
    List<LoteMedicamento> findByMedicamentoIdMedicamentoAndStockActualGreaterThanOrderByFechaVencimientoAsc(Integer idMedicamento, Integer stock);
    List<LoteMedicamento> findByStockActualGreaterThan(Integer stock);
    @Query("SELECT l FROM LoteMedicamento l JOIN FETCH l.medicamento ORDER BY l.medicamento.idMedicamento, l.fechaVencimiento ASC")
    List<LoteMedicamento> findAllWithMedicamento();
    boolean existsByMedicamentoIdMedicamentoAndNumeroLoteIgnoreCase(Integer idMedicamento, String numeroLote);
}
