package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.ConsultaDeStockDePool;
import com.costumi.backend.disfraces.dominio.Disfraz;
import com.costumi.backend.disfraces.dominio.DisfrazRepository;
import com.costumi.backend.disfraces.dominio.EjeDePrenda;
import com.costumi.backend.disfraces.dominio.PoolDeSlot;
import com.costumi.backend.disfraces.dominio.Slot;
import com.costumi.backend.inventario.ConsultaDeInventario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Casos de uso de Disfraces (Capa 3), acotados a la empresa (tenant). */
@Service
class DisfrazService implements CrearDisfraz, ConsultarDisfraces, ConsultarDisponibilidadDeDisfraz {

	private final DisfrazRepository disfraces;
	private final ConsultaDeInventario inventario;

	DisfrazService(DisfrazRepository disfraces, ConsultaDeInventario inventario) {
		this.disfraces = disfraces;
		this.inventario = inventario;
	}

	@Override
	@Transactional
	public Disfraz ejecutar(CrearDisfrazComando comando) {
		Disfraz disfraz = switch (comando.modo()) {
			case UNIDAD_FIJA -> Disfraz.unidadFija(comando.empresaId(), comando.nombre(), comando.prendaFijaId());
			case POR_PARTES -> Disfraz.porPartes(comando.empresaId(), comando.nombre(), aSlots(comando.slots()));
		};
		return disfraces.guardar(disfraz);
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
