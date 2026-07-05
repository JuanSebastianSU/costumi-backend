package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.AjusteDeInventario;
import com.costumi.backend.inventario.StockInsuficiente;
import com.costumi.backend.inventario.dominio.GrupoDeStock;
import com.costumi.backend.inventario.dominio.GrupoDeStockRepository;
import com.costumi.backend.inventario.dominio.PrendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Implementa la API pública de ajuste de inventario (baja de stock), acotada al tenant. */
@Service
class AjusteDeInventarioService implements AjusteDeInventario {

	private final PrendaRepository prendas;
	private final GrupoDeStockRepository grupos;

	AjusteDeInventarioService(PrendaRepository prendas, GrupoDeStockRepository grupos) {
		this.prendas = prendas;
		this.grupos = grupos;
	}

	@Override
	@Transactional
	public void descontarDisponibles(UUID empresaId, UUID prendaId, int cantidad) {
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad a descontar debe ser mayor a 0");
		}
		boolean delTenant = prendas.buscarPorId(prendaId).filter(p -> p.empresaId().equals(empresaId)).isPresent();
		if (!delTenant) {
			throw new StockInsuficiente(prendaId);
		}
		List<GrupoDeStock> deLaPrenda = grupos.listarPorPrenda(prendaId);
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
}
