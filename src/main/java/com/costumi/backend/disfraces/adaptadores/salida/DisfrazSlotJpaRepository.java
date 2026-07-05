package com.costumi.backend.disfraces.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

interface DisfrazSlotJpaRepository extends JpaRepository<DisfrazSlotJpaEntity, UUID> {

	List<DisfrazSlotJpaEntity> findByDisfrazIdOrderByOrden(UUID disfrazId);

	@Transactional
	void deleteByDisfrazId(UUID disfrazId);
}
