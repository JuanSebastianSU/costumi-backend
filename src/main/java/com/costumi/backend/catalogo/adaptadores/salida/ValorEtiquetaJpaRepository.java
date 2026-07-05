package com.costumi.backend.catalogo.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ValorEtiquetaJpaRepository extends JpaRepository<ValorEtiquetaJpaEntity, UUID> {

	List<ValorEtiquetaJpaEntity> findByTipoEtiquetaId(UUID tipoEtiquetaId);
}
