package com.costumi.backend.identidad.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Repositorio Spring Data sobre la entidad JPA (detalle de infraestructura). */
interface EmpresaJpaRepository extends JpaRepository<EmpresaJpaEntity, UUID> {
}
