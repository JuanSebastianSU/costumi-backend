package com.costumi.backend.disfraces.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface DisfrazSlotJpaRepository extends JpaRepository<DisfrazSlotJpaEntity, UUID> {

	List<DisfrazSlotJpaEntity> findByDisfrazIdOrderByOrden(UUID disfrazId);

	/**
	 * Carga en UNA sola query los slots de varios disfraces (evita el N+1 al listar por empresa).
	 * Con {@code hibernate.default_batch_fetch_size} activo, las etiquetas de cada slot se cargan por lotes.
	 */
	List<DisfrazSlotJpaEntity> findByDisfrazIdInOrderByDisfrazIdAscOrdenAsc(Collection<UUID> disfrazIds);

	@Transactional
	void deleteByDisfrazId(UUID disfrazId);
}
