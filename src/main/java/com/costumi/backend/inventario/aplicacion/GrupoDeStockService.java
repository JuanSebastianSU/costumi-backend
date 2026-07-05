package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.GrupoDeStock;
import com.costumi.backend.inventario.dominio.GrupoDeStockRepository;
import com.costumi.backend.inventario.dominio.PrendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Casos de uso de Grupos de stock, acotados a la empresa (tenant). */
@Service
class GrupoDeStockService implements CrearGrupoDeStock, ConsultarGruposDeStock, MoverUnidades {

	private final PrendaRepository prendas;
	private final GrupoDeStockRepository grupos;

	GrupoDeStockService(PrendaRepository prendas, GrupoDeStockRepository grupos) {
		this.prendas = prendas;
		this.grupos = grupos;
	}

	@Override
	@Transactional
	public GrupoDeStock ejecutar(CrearGrupoDeStockComando comando) {
		exigirPrendaDelTenant(comando.empresaId(), comando.prendaId());
		return grupos.guardar(GrupoDeStock.crear(
				comando.empresaId(), comando.prendaId(), comando.etiqueta(), comando.cantidadInicial()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<GrupoDeStock> dePrenda(UUID empresaId, UUID prendaId) {
		exigirPrendaDelTenant(empresaId, prendaId);
		return grupos.listarPorPrenda(prendaId);
	}

	@Override
	@Transactional
	public GrupoDeStock ejecutar(MoverUnidadesComando comando) {
		GrupoDeStock grupo = grupos.buscarPorId(comando.grupoId())
				.filter(g -> g.empresaId().equals(comando.empresaId()))
				.orElseThrow(() -> new GrupoDeStockNoEncontrado(comando.grupoId()));
		grupo.mover(comando.desde(), comando.hacia(), comando.cantidad());
		return grupos.guardar(grupo);
	}

	private void exigirPrendaDelTenant(UUID empresaId, UUID prendaId) {
		prendas.buscarPorId(prendaId)
				.filter(prenda -> prenda.empresaId().equals(empresaId))
				.orElseThrow(() -> new PrendaNoEncontrada(prendaId));
	}
}
