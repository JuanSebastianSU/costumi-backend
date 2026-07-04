package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.EstadoEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repositorio Spring Data sobre la entidad JPA (detalle de infraestructura). */
interface EmpresaJpaRepository extends JpaRepository<EmpresaJpaEntity, UUID> {

	List<EmpresaJpaEntity> findByEstado(EstadoEmpresa estado);
}
