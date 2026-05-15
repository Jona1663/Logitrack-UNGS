package com.logitrack.sistema_logistica.repository;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import java.time.LocalDateTime;

public class EnvioSpecifications {

    public static Specification<Envio> tieneEstado(EstadoEnvio estado) {
        return (root, query, criteriaBuilder) -> {
            if (estado == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("estadoActual"), estado);
        };
    }

    public static Specification<Envio> esDeTipoGrano(String tipoGrano) {
        return (root, query, criteriaBuilder) -> {
            if (tipoGrano == null || tipoGrano.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("tipoGrano"), tipoGrano);
        };
    }

    public static Specification<Envio> fechaCreacionEntre(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return (root, query, criteriaBuilder) -> {
            if (fechaInicio == null || fechaFin == null) {
                return null;
            }
            return criteriaBuilder.and(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("fechaCreacion"), fechaInicio),
                    criteriaBuilder.lessThan(root.get("fechaCreacion"), fechaFin));
        };
    }

    public static Specification<Envio> contieneTermino(String termino) {
        return (root, query, criteriaBuilder) -> {
            if (termino == null || termino.isBlank()) {
                return null;
            }

            String likePattern = "%" + termino.toLowerCase() + "%";
            Join<Object, Object> origenJoin = root.join("origen", JoinType.LEFT);
            Join<Object, Object> destinoJoin = root.join("destino", JoinType.LEFT);

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("idEnvio")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("cpe")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("tipoGrano").as(String.class)), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(origenJoin.get("nombreLugar")), likePattern),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(origenJoin.join("empresa", JoinType.LEFT).get("razonSocial")),
                            likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(destinoJoin.get("nombreLugar")), likePattern),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(destinoJoin.join("empresa", JoinType.LEFT).get("razonSocial")),
                            likePattern));
        };
    }
}
