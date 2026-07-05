package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.inventario.dominio.GrupoDeStockRepository;
import com.costumi.backend.inventario.dominio.Prenda;
import com.costumi.backend.inventario.dominio.PrendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Implementa la API pública de consulta de inventario (disponibilidad), acotada al tenant. */
@Service
class ConsultaDeInventarioService implements ConsultaDeInventario {

	private final PrendaRepository prendas;
	private final GrupoDeStockRepository grupos;

	ConsultaDeInventarioService(PrendaRepository prendas, GrupoDeStockRepository grupos) {
		this.prendas = prendas;
		this.grupos = grupos;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean prendaTieneStockDisponible(UUID empresaId, UUID prendaId) {
		return prendas.buscarPorId(prendaId)
				.filter(prenda -> prenda.empresaId().equals(empresaId))
				.map(prenda -> tieneStock(prenda.id()))
				.orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean poolTieneStockDisponible(UUID empresaId, UUID categoriaId, Map<UUID, Set<UUID>> etiquetasPermitidas) {
		return prendas.listarPorEmpresa(empresaId).stream()
				.filter(prenda -> prenda.categoriaId().equals(categoriaId))
				.filter(prenda -> cumpleEtiquetas(prenda, etiquetasPermitidas))
				.anyMatch(prenda -> tieneStock(prenda.id()));
	}

	private boolean tieneStock(UUID prendaId) {
		return grupos.listarPorPrenda(prendaId).stream().anyMatch(grupo -> grupo.disponibles() > 0);
	}

	private static boolean cumpleEtiquetas(Prenda prenda, Map<UUID, Set<UUID>> etiquetasPermitidas) {
		Map<UUID, UUID> valoresDeLaPrenda = prenda.etiquetas().valores();
		for (Map.Entry<UUID, Set<UUID>> exigencia : etiquetasPermitidas.entrySet()) {
			UUID valorDeLaPrenda = valoresDeLaPrenda.get(exigencia.getKey());
			if (valorDeLaPrenda == null || !exigencia.getValue().contains(valorDeLaPrenda)) {
				return false;
			}
		}
		return true;
	}
}
