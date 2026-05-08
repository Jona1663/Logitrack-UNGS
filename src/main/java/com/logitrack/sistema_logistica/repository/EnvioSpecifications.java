package com.logitrack.sistema_logistica.repository;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.Estado_Envio;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import java.time.LocalDateTime;

public class EnvioSpecifications {

    public static Specification<Envio> tieneEstado(Estado_Envio estado) {
        return (root, query, criteriaBuilder) -> {
            if (estado == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("estado_actual"), estado);
        };
    }

    public static Specification<Envio> fechaCreacionEntre(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return (root, query, criteriaBuilder) -> {
            if (fechaInicio == null || fechaFin == null) {
                return null;
            }
            return criteriaBuilder.and(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("fecha_creacion"), fechaInicio),
                    criteriaBuilder.lessThan(root.get("fecha_creacion"), fechaFin)
            );
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
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("id_envio")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("cpe")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("tipo_grano").as(String.class)), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(origenJoin.get("nombre_lugar")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(origenJoin.join("empresa", JoinType.LEFT).get("razon_social")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(destinoJoin.get("nombre_lugar")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(destinoJoin.join("empresa", JoinType.LEFT).get("razon_social")), likePattern)
            );
        };
    }
}
