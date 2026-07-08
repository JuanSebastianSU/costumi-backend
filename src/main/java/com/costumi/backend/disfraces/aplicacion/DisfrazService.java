package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.ConsultaDeStockDePool;
import com.costumi.backend.disfraces.dominio.Disfraz;
import com.costumi.backend.disfraces.dominio.DisfrazRepository;
import com.costumi.backend.disfraces.dominio.EjeDePrenda;
import com.costumi.backend.disfraces.dominio.PoolDeSlot;
import com.costumi.backend.disfraces.dominio.Slot;
import com.costumi.backend.catalogo.ConsultaDeTaxonomia;
import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.rentas.RegistroDeRentas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Casos de uso de Disfraces (Capa 3), acotados a la empresa (tenant). */
@Service
class DisfrazService implements CrearDisfraz, ConsultarDisfraces, ConsultarDisponibilidadDeDisfraz, RentarDisfraz {

	private final DisfrazRepository disfraces;
	private final ConsultaDeInventario inventario;
	private final ConsultaDeTaxonomia taxonomia;
	private final RegistroDeRentas rentas;

	DisfrazService(DisfrazRepository disfraces, ConsultaDeInventario inventario, ConsultaDeTaxonomia taxonomia,
			RegistroDeRentas rentas) {
		this.disfraces = disfraces;
		this.inventario = inventario;
		this.taxonomia = taxonomia;
		this.rentas = rentas;
	}

	@Override
	@Transactional
	public Disfraz ejecutar(CrearDisfrazComando comando) {
		validarReferenciasDelTenant(comando);
		Disfraz disfraz = switch (comando.modo()) {
			case UNIDAD_FIJA -> Disfraz.unidadFija(comando.empresaId(), comando.nombre(), comando.prendaFijaId());
			case POR_PARTES -> Disfraz.porPartes(comando.empresaId(), comando.nombre(), aSlots(comando.slots()));
		};
		return disfraces.guardar(disfraz);
	}

	/** §5.4: toda referencia por id (prenda fija, categoría y valores del pool) debe ser del tenant. */
	private void validarReferenciasDelTenant(CrearDisfrazComando comando) {
		UUID empresaId = comando.empresaId();
		switch (comando.modo()) {
			case UNIDAD_FIJA -> exigirPrendaDelTenant(empresaId, comando.prendaFijaId());
			case POR_PARTES -> comando.slots().forEach(slot -> validarSlotDelTenant(empresaId, slot));
		}
	}

	private void validarSlotDelTenant(UUID empresaId, SlotComando slot) {
		switch (slot.ejePrenda()) {
			case FIJA -> exigirPrendaDelTenant(empresaId, slot.prendaFijaId());
			case PERSONALIZABLE -> {
				PoolComando pool = slot.pool();
				if (pool == null || pool.categoriaId() == null
						|| !taxonomia.categoriaExiste(empresaId, pool.categoriaId())) {
					throw new IllegalArgumentException("La categoría del pool no existe en esta empresa");
				}
				pool.etiquetasPermitidas().forEach((tipo, valores) -> valores.forEach(valor -> {
					if (!taxonomia.valorPerteneceATipo(empresaId, tipo, valor)) {
						throw new IllegalArgumentException("Un valor permitido del pool no pertenece a su tipo en esta empresa");
					}
				}));
			}
		}
	}

	private void exigirPrendaDelTenant(UUID empresaId, UUID prendaId) {
		if (prendaId == null || !inventario.prendaExiste(empresaId, prendaId)) {
			throw new IllegalArgumentException("La prenda fija no existe en esta empresa");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<Disfraz> deEmpresa(UUID empresaId) {
		return disfraces.listarPorEmpresa(empresaId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean estaDisponible(UUID empresaId, UUID disfrazId) {
		Disfraz disfraz = disfraces.buscarPorId(disfrazId)
				.filter(d -> d.empresaId().equals(empresaId))
				.orElseThrow(() -> new DisfrazNoEncontrado(disfrazId));
		return disfraz.estaDisponible(consultaDeStockPara(empresaId));
	}

	@Override
	@Transactional
	public UUID ejecutar(RentarDisfrazComando comando) {
		UUID empresaId = comando.empresaId();
		Disfraz disfraz = disfraces.buscarPorId(comando.disfrazId())
				.filter(d -> d.empresaId().equals(empresaId))
				.orElseThrow(() -> new DisfrazNoEncontrado(comando.disfrazId()));
		Map<Integer, UUID> seleccionPorOrden = new HashMap<>();
		if (comando.selecciones() != null) {
			for (RentarDisfrazComando.SeleccionDeSlot seleccion : comando.selecciones()) {
				seleccionPorOrden.put(seleccion.orden(), seleccion.prendaId());
			}
		}
		List<RegistroDeRentas.ItemDeRenta> items = new ArrayList<>();
		switch (disfraz.modo()) {
			case UNIDAD_FIJA -> items.add(itemDeRenta(empresaId, disfraz.prendaFijaId()));
			case POR_PARTES -> {
				for (Slot slot : disfraz.slots()) {
					boolean elegido = seleccionPorOrden.containsKey(slot.orden());
					// Los slots opcionales solo entran si el cliente los eligió; los obligatorios siempre.
					if (!slot.esObligatorio() && !elegido) {
						continue;
					}
					items.add(itemDeRenta(empresaId, resolverPrenda(empresaId, slot, seleccionPorOrden.get(slot.orden()))));
				}
			}
		}
		if (items.isEmpty()) {
			throw new IllegalArgumentException("El disfraz no resolvió ningún artículo para rentar");
		}
		return rentas.registrar(empresaId, comando.sucursalId(), comando.clienteId(), comando.fechaRetiro(),
				comando.fechaDevolucion(), null, items);
	}

	/** Prenda concreta de un slot: la fija, o la elegida por el cliente validada contra el pool (RF-2.3). */
	private UUID resolverPrenda(UUID empresaId, Slot slot, UUID prendaElegida) {
		if (slot.ejePrenda() == EjeDePrenda.FIJA) {
			return slot.prendaFijaId();
		}
		if (prendaElegida == null) {
			throw new IllegalArgumentException("Falta elegir la prenda del slot '" + slot.nombre() + "'");
		}
		PoolDeSlot pool = slot.pool();
		if (!inventario.prendaEnPool(empresaId, prendaElegida, pool.categoriaId(), pool.etiquetasPermitidas())) {
			throw new IllegalArgumentException(
					"La prenda elegida no pertenece al pool del slot '" + slot.nombre() + "'");
		}
		return prendaElegida;
	}

	private RegistroDeRentas.ItemDeRenta itemDeRenta(UUID empresaId, UUID prendaId) {
		BigDecimal precio = inventario.precioRenta(empresaId, prendaId)
				.orElseThrow(() -> new IllegalArgumentException("La prenda del disfraz no tiene precio de renta"));
		return new RegistroDeRentas.ItemDeRenta(prendaId, 1, precio);
	}

	/** Puente del puerto de Inventario al puerto de dominio del Disfraz, fijado al tenant. */
	private ConsultaDeStockDePool consultaDeStockPara(UUID empresaId) {
		return new ConsultaDeStockDePool() {
			@Override
			public boolean prendaTieneStock(UUID prendaId) {
				return inventario.prendaTieneStockDisponible(empresaId, prendaId);
			}

			@Override
			public boolean poolTieneStock(UUID categoriaId, Map<UUID, Set<UUID>> etiquetasPermitidas) {
				return inventario.poolTieneStockDisponible(empresaId, categoriaId, etiquetasPermitidas);
			}
		};
	}

	private static List<Slot> aSlots(List<SlotComando> comandos) {
		return comandos.stream().map(DisfrazService::aSlot).toList();
	}

	private static Slot aSlot(SlotComando c) {
		if (c.ejePrenda() == EjeDePrenda.FIJA) {
			return Slot.conPrendaFija(c.orden(), c.nombre(), c.ejeTalla(), c.tallaFija(), c.prendaFijaId(), c.opcional());
		}
		PoolComando pool = c.pool();
		if (pool == null) {
			throw new IllegalArgumentException("Un slot personalizable requiere pool");
		}
		return Slot.personalizable(c.orden(), c.nombre(), c.ejeTalla(), c.tallaFija(),
				PoolDeSlot.de(pool.categoriaId(), pool.etiquetasPermitidas()), c.opcional());
	}
}
