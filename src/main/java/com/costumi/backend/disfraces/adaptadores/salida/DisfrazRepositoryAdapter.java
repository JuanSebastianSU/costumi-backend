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
		cabeceras.save(new DisfrazJpaEntity(disfraz.id(), disfraz.empresaId(), disfraz.nombre(), disfraz.categoriaId(),
				disfraz.activo(), disfraz.precioRentaGeneral(), disfraz.precioVentaGeneral(), disfraz.fotoUrl(),
				disfraz.tipo()));
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
	public com.costumi.backend.compartido.Pagina<Disfraz> listarPorEmpresa(UUID empresaId, String buscar, UUID categoriaId,
			com.costumi.backend.compartido.SolicitudDePagina pagina) {
		org.springframework.data.domain.Page<DisfrazJpaEntity> page = cabeceras.buscarPagina(empresaId,
				buscar == null || buscar.isBlank() ? null : buscar.trim(), categoriaId,
				org.springframework.data.domain.PageRequest.of(pagina.pagina(), pagina.tamano()));
		return com.costumi.backend.compartido.Pagina.de(conSlots(page.getContent()), page.getTotalElements(), pagina);
	}

	@Override
	public List<Disfraz> listarPorEmpresa(UUID empresaId) {
		return conSlots(cabeceras.findByEmpresaId(empresaId));
	}

	/** Rehidrata las cabeceras con sus slots en UNA sola query (evita el N+1 de un findByDisfrazId por cabecera). */
	private List<Disfraz> conSlots(List<DisfrazJpaEntity> lista) {
		if (lista.isEmpty()) {
			return List.of();
		}
		List<UUID> ids = lista.stream().map(DisfrazJpaEntity::getId).toList();
		Map<UUID, List<Slot>> slotsPorDisfraz = slots.findByDisfrazIdInOrderByDisfrazIdAscOrdenAsc(ids).stream()
				.collect(Collectors.groupingBy(DisfrazSlotJpaEntity::getDisfrazId, LinkedHashMap::new,
						Collectors.mapping(DisfrazRepositoryAdapter::aSlotDominio, Collectors.toList())));
		return lista.stream()
				.map(cabecera -> Disfraz.rehidratar(cabecera.getId(), cabecera.getEmpresaId(), cabecera.getNombre(),
						cabecera.getCategoriaId(), slotsPorDisfraz.getOrDefault(cabecera.getId(), List.of()),
						cabecera.isActivo(), cabecera.getPrecioRentaGeneral(), cabecera.getPrecioVentaGeneral(),
						cabecera.getFotoUrl(), cabecera.getTipo()))
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
				cabecera.getCategoriaId(), slotsDominio, cabecera.isActivo(), cabecera.getPrecioRentaGeneral(),
				cabecera.getPrecioVentaGeneral(), cabecera.getFotoUrl(), cabecera.getTipo());
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
