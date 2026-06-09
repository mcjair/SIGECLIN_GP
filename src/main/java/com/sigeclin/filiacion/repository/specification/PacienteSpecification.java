package com.sigeclin.filiacion.repository.specification;

import com.sigeclin.filiacion.model.Paciente;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class PacienteSpecification {

    public static Specification<Paciente> conFiltro(String search, String servicioFiltro) {
        return (root, query, cb) -> {
            jakarta.persistence.criteria.Predicate predicate = cb.conjunction();
            
            if (StringUtils.hasText(servicioFiltro) && !servicioFiltro.equalsIgnoreCase("ADMIN")) {
                predicate = cb.and(predicate, cb.equal(cb.upper(root.get("servicioSolicitado")), servicioFiltro.toUpperCase()));
            }

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("numeroDocumento")), searchPattern),
                    cb.like(cb.lower(root.get("numeroHistoriaClinica")), searchPattern),
                    cb.like(cb.lower(root.get("nombres")), searchPattern),
                    cb.like(cb.lower(root.get("apellidoPaterno")), searchPattern),
                    cb.like(cb.lower(root.get("apellidoMaterno")), searchPattern)
                );
                predicate = cb.and(predicate, searchPredicate);
            }
            
            return predicate;
        };
    }
}
