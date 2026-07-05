package com.costumi.backend.catalogo.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CategoriaJpaRepository extends JpaRepository<CategoriaJpaEntity, UUID> {

	List<CategoriaJpaEntity> findByEmpresaId(UUID empresaId);
}
