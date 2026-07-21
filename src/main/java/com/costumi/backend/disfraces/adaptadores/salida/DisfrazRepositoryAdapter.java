package com.costumi.backend.disfraces.adaptadores.salida;

import com.costumi.backend.disfraces.dominio.Disfraz;
import com.costumi.backend.disfraces.dominio.DisfrazRepository;
import com.costumi.backend.disfraces.dominio.PoolDeSlot;
import com.costumi.backend.disfraces.dominio.Slot;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Adaptador de salida: persiste el agregado Disfraz (cabecera + slots + pools) con JPA. */
@Repository
class DisfrazRepositoryAdapter implements DisfrazRepository {

	private final DisfrazJpaRepository cabeceras;
	private final DisfrazSlotJpaRepository slots;

	DisfrazRepositoryAdapter(DisfrazJpaRepository cabeceras, DisfrazSlotJpaRepository slots) {
		this.cabeceras = cabeceras;
		this.slots = slots;
	}

	@Override
	public Disfraz guardar(Disfraz disfraz) {
		cabeceras.save(new DisfrazJpaEntity(disfraz.id(), disfraz.empresaId(), disfraz.nombre(), disfraz.activo(),
				disfraz.precioRentaGeneral(), disfraz.fotoUrl()));
		slots.deleteByDisfrazId(disfraz.id());
		for (Slot slot : disfraz.slots()) {
			slots.save(aEntidad(disfraz.id(), slot));
		}
		return disfraz;
	}

	@Override
	public Optional<Disfraz> buscarPorId(UUID id) {
		return cabeceras.findFirstById(id).map(this::aDominio);
	}

	@Override
	public List<Disfraz> listarPorEmpresa(UUID empresaId) {
		List<DisfrazJpaEntity> lista = cabeceras.findByEmpresaId(empresaId);
		if (lista.isEmpty()) {
			return List.of();
		}
		// Una sola query para TODOS los slots de estos disfraces (evita el N+1 de un findByDisfrazId por cabecera).
		List<UUID> ids = lista.stream().map(DisfrazJpaEntity::getId).toList();
		Map<UUID, List<Slot>> slotsPorDisfraz = slots.findByDisfrazIdInOrderByDisfrazIdAscOrdenAsc(ids).stream()
				.collect(Collectors.groupingBy(DisfrazSlotJpaEntity::getDisfrazId, LinkedHashMap::new,
						Collectors.mapping(DisfrazRepositoryAdapter::aSlotDominio, Collectors.toList())));
		return lista.stream()
				.map(cabecera -> Disfraz.rehidratar(cabecera.getId(), cabecera.getEmpresaId(), cabecera.getNombre(),
						slotsPorDisfraz.getOrDefault(cabecera.getId(), List.of()), cabecera.isActivo(),
						cabecera.getPrecioRentaGeneral(), cabecera.getFotoUrl()))
				.toList();
	}

	private static DisfrazSlotJpaEntity aEntidad(UUID disfrazId, Slot slot) {
		UUID categoriaId = slot.pool() == null ? null : slot.pool().categoriaId();
		Set<EtiquetaDeSlotEmbeddable> etiquetas = new LinkedHashSet<>();
		if (slot.pool() != null) {
			slot.pool().etiquetasPermitidas().forEach((tipo, valores) ->
					valores.forEach(valor -> etiquetas.add(new EtiquetaDeSlotEmbeddable(tipo, valor))));
		}
		Set<UUID> prendasOpcion = new LinkedHashSet<>(slot.prendasOpcion());
		return new DisfrazSlotJpaEntity(UUID.randomUUID(), disfrazId, slot.orden(), slot.nombre(),
				slot.ejePrenda(), slot.prendaFijaId(), categoriaId, slot.opcional(), etiquetas, prendasOpcion);
	}

	private Disfraz aDominio(DisfrazJpaEntity cabecera) {
		List<Slot> slotsDominio = slots.findByDisfrazIdOrderByOrden(cabecera.getId()).stream()
				.map(DisfrazRepositoryAdapter::aSlotDominio)
				.toList();
		return Disfraz.rehidratar(cabecera.getId(), cabecera.getEmpresaId(), cabecera.getNombre(),
				slotsDominio, cabecera.isActivo(), cabecera.getPrecioRentaGeneral(), cabecera.getFotoUrl());
	}

	private static Slot aSlotDominio(DisfrazSlotJpaEntity e) {
		PoolDeSlot pool = null;
		if (e.getCategoriaId() != null) {
			Map<UUID, Set<UUID>> etiquetas = e.getEtiquetasPermitidas().stream().collect(Collectors.groupingBy(
					EtiquetaDeSlotEmbeddable::getTipoEtiquetaId, LinkedHashMap::new,
					Collectors.mapping(EtiquetaDeSlotEmbeddable::getValorEtiquetaId, Collectors.toCollection(LinkedHashSet::new))));
			pool = PoolDeSlot.de(e.getCategoriaId(), etiquetas);
		}
		List<UUID> prendasOpcion = List.copyOf(e.getPrendasOpcion());
		return Slot.rehidratar(e.getOrden(), e.getNombre(), e.getEjePrenda(), e.getPrendaFijaId(), pool,
				prendasOpcion, e.isOpcional());
	}
}
