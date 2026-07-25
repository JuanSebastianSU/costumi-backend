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

	/**
	 * Cuántos disfraces de la empresa usan la prenda, como pieza FIJA o como opción EXPLÍCITA de un slot
	 * (para avisar del impacto antes de archivarla). El pool no cuenta: no referencia prendas por id, se
	 * resuelve por categoría+etiquetas. Nativa y acotada por {@code empresa_id} (join a disfraz).
	 */
	@org.springframework.data.jpa.repository.Query(value = """
			select count(distinct s.disfraz_id)
			from disfraz_slot s
			join disfraz d on d.id = s.disfraz_id
			where d.empresa_id = :empresaId
			  and (s.prenda_fija_id = :prendaId
			       or exists (select 1 from disfraz_slot_prenda_opcion o
			                  where o.slot_id = s.id and o.prenda_id = :prendaId))
			""", nativeQuery = true)
	long contarQueUsanPrenda(@org.springframework.data.repository.query.Param("empresaId") UUID empresaId,
			@org.springframework.data.repository.query.Param("prendaId") UUID prendaId);
}
