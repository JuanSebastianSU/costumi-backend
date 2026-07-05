package com.costumi.backend.catalogo.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TipoEtiquetaJpaRepository extends JpaRepository<TipoEtiquetaJpaEntity, UUID> {

	List<TipoEtiquetaJpaEntity> findByEmpresaId(UUID empresaId);
}
