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
		cabeceras.save(new DisfrazJpaEntity(disfraz.id(), disfraz.empresaId(), disfraz.nombre(), disfraz.modo(),
				disfraz.prendaFijaId()));
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
		return cabeceras.findByEmpresaId(empresaId).stream().map(this::aDominio).toList();
	}

	private static DisfrazSlotJpaEntity aEntidad(UUID disfrazId, Slot slot) {
		UUID categoriaId = slot.pool() == null ? null : slot.pool().categoriaId();
		Set<EtiquetaDeSlotEmbeddable> etiquetas = new LinkedHashSet<>();
		if (slot.pool() != null) {
			slot.pool().etiquetasPermitidas().forEach((tipo, valores) ->
					valores.forEach(valor -> etiquetas.add(new EtiquetaDeSlotEmbeddable(tipo, valor))));
		}
		return new DisfrazSlotJpaEntity(UUID.randomUUID(), disfrazId, slot.orden(), slot.nombre(), slot.ejeTalla(),
				slot.tallaFija(), slot.ejePrenda(), slot.prendaFijaId(), categoriaId, slot.opcional(), etiquetas);
	}

	private Disfraz aDominio(DisfrazJpaEntity cabecera) {
		List<Slot> slotsDominio = slots.findByDisfrazIdOrderByOrden(cabecera.getId()).stream()
				.map(DisfrazRepositoryAdapter::aSlotDominio)
				.toList();
		return Disfraz.rehidratar(cabecera.getId(), cabecera.getEmpresaId(), cabecera.getNombre(), cabecera.getModo(),
				cabecera.getPrendaFijaId(), slotsDominio);
	}

	private static Slot aSlotDominio(DisfrazSlotJpaEntity e) {
		PoolDeSlot pool = null;
		if (e.getCategoriaId() != null) {
			Map<UUID, Set<UUID>> etiquetas = e.getEtiquetasPermitidas().stream().collect(Collectors.groupingBy(
					EtiquetaDeSlotEmbeddable::getTipoEtiquetaId, LinkedHashMap::new,
					Collectors.mapping(EtiquetaDeSlotEmbeddable::getValorEtiquetaId, Collectors.toCollection(LinkedHashSet::new))));
			pool = PoolDeSlot.de(e.getCategoriaId(), etiquetas);
		}
		return Slot.rehidratar(e.getOrden(), e.getNombre(), e.getEjeTalla(), e.getTallaFija(), e.getEjePrenda(),
				e.getPrendaFijaId(), pool, e.isOpcional());
	}
}
