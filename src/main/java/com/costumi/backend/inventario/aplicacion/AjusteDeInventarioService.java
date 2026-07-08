package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.AjusteDeInventario;
import com.costumi.backend.inventario.StockInsuficiente;
import com.costumi.backend.inventario.dominio.EstadoUnidad;
import com.costumi.backend.inventario.dominio.GrupoDeStock;
import com.costumi.backend.inventario.dominio.GrupoDeStockRepository;
import com.costumi.backend.inventario.dominio.PrendaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Implementa la API pública de ajuste de inventario (baja de stock), acotada al tenant. */
@Service
class AjusteDeInventarioService implements AjusteDeInventario {

	private final PrendaRepository prendas;
	private final GrupoDeStockRepository grupos;

	@PersistenceContext
	private EntityManager em;

	AjusteDeInventarioService(PrendaRepository prendas, GrupoDeStockRepository grupos) {
		this.prendas = prendas;
		this.grupos = grupos;
	}

	/**
	 * Lock de transacción por prenda (se libera al commit): serializa las bajas/movimientos de stock de
	 * una misma prenda para evitar la <b>sobreventa</b> por read-then-write concurrente (como en renta).
	 */
	private void bloquearPrenda(UUID prendaId) {
		em.createNativeQuery(
						"select count(*) from (select pg_advisory_xact_lock(hashtext('prenda:' || cast(:p as text)))) as l")
				.setParameter("p", prendaId.toString())
				.getSingleResult();
	}

	@Override
	@Transactional
	public void descontarDisponibles(UUID empresaId, UUID sucursalId, UUID prendaId, int cantidad) {
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad a descontar debe ser mayor a 0");
		}
		bloquearPrenda(prendaId);
		boolean delTenant = prendas.buscarPorId(prendaId).filter(p -> p.empresaId().equals(empresaId)).isPresent();
		if (!delTenant) {
			throw new StockInsuficiente(prendaId);
		}
		List<GrupoDeStock> deLaPrenda = grupos.listarPorPrendaYSucursal(prendaId, sucursalId);
		int totalDisponible = deLaPrenda.stream().mapToInt(GrupoDeStock::disponibles).sum();
		if (totalDisponible < cantidad) {
			throw new StockInsuficiente(prendaId);
		}
		int restante = cantidad;
		for (GrupoDeStock grupo : deLaPrenda) {
			if (restante == 0) {
				break;
			}
			int aDescontar = Math.min(grupo.disponibles(), restante);
			if (aDescontar > 0) {
				grupo.darDeBaja(aDescontar);
				grupos.guardar(grupo);
				restante -= aDescontar;
			}
		}
	}

	@Override
	@Transactional
	public void reingresarDisponibles(UUID empresaId, UUID sucursalId, UUID prendaId, int cantidad) {
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad a reingresar debe ser mayor a 0");
		}
		bloquearPrenda(prendaId);
		boolean delTenant = prendas.buscarPorId(prendaId).filter(p -> p.empresaId().equals(empresaId)).isPresent();
		if (!delTenant) {
			throw new StockInsuficiente(prendaId);
		}
		List<GrupoDeStock> deLaPrenda = grupos.listarPorPrendaYSucursal(prendaId, sucursalId);
		if (deLaPrenda.isEmpty()) {
			throw new StockInsuficiente(prendaId); // no hay grupo en la sucursal donde reingresar
		}
		GrupoDeStock grupo = deLaPrenda.get(0);
		grupo.reabastecer(cantidad);
		grupos.guardar(grupo);
	}

	@Override
	@Transactional
	public void procesarRetornoDeRenta(UUID empresaId, UUID sucursalId, UUID prendaId, int danadas, int enLimpieza,
			int perdidas) {
		bloquearPrenda(prendaId);
		boolean delTenant = prendas.buscarPorId(prendaId).filter(p -> p.empresaId().equals(empresaId)).isPresent();
		if (!delTenant) {
			throw new StockInsuficiente(prendaId);
		}
		List<GrupoDeStock> deLaPrenda = grupos.listarPorPrendaYSucursal(prendaId, sucursalId);
		moverDesdeDisponible(deLaPrenda, prendaId, EstadoUnidad.DANADA, danadas);
		moverDesdeDisponible(deLaPrenda, prendaId, EstadoUnidad.EN_LIMPIEZA, enLimpieza);
		moverDesdeDisponible(deLaPrenda, prendaId, EstadoUnidad.PERDIDA, perdidas);
	}

	/** Mueve {@code cantidad} unidades de DISPONIBLE al estado destino, repartiendo entre los grupos. */
	private void moverDesdeDisponible(List<GrupoDeStock> grupos, UUID prendaId, EstadoUnidad destino, int cantidad) {
		if (cantidad <= 0) {
			return;
		}
		int restante = cantidad;
		for (GrupoDeStock grupo : grupos) {
			if (restante == 0) {
				break;
			}
			int aMover = Math.min(grupo.disponibles(), restante);
			if (aMover > 0) {
				grupo.mover(EstadoUnidad.DISPONIBLE, destino, aMover);
				this.grupos.guardar(grupo);
				restante -= aMover;
			}
		}
		if (restante > 0) {
			throw new StockInsuficiente(prendaId);
		}
	}
}
