package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.catalogo.ConsultaDeTaxonomia;
import com.costumi.backend.inventario.dominio.CombinacionDeVariante;
import com.costumi.backend.inventario.dominio.GrupoDeStock;
import com.costumi.backend.inventario.dominio.GrupoDeStockRepository;
import com.costumi.backend.inventario.dominio.PrendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Casos de uso de Grupos de stock (variantes), acotados a la empresa (tenant). */
@Service
class GrupoDeStockService implements CrearGrupoDeStock, ConsultarGruposDeStock, MoverUnidades {

	private final PrendaRepository prendas;
	private final GrupoDeStockRepository grupos;
	private final ConsultaDeTaxonomia taxonomia;

	GrupoDeStockService(PrendaRepository prendas, GrupoDeStockRepository grupos, ConsultaDeTaxonomia taxonomia) {
		this.prendas = prendas;
		this.grupos = grupos;
		this.taxonomia = taxonomia;
	}

	@Override
	@Transactional
	public GrupoDeStock ejecutar(CrearGrupoDeStockComando comando) {
		exigirPrendaDelTenant(comando.empresaId(), comando.prendaId());
		CombinacionDeVariante combinacion = validarCombinacion(comando.empresaId(), comando.combinacion());
		exigirVarianteNoDuplicada(comando.prendaId(), combinacion);
		return grupos.guardar(GrupoDeStock.crear(
				comando.empresaId(), comando.prendaId(), combinacion, comando.cantidadInicial()));
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

	/**
	 * Convierte las selecciones en una {@link CombinacionDeVariante} <b>real</b>: cada dimensión debe
	 * ser un tipo que define variante en la empresa y cada valor debe pertenecer a ese tipo; una
	 * dimensión no se puede repetir. Una lista vacía es la variante única.
	 */
	private CombinacionDeVariante validarCombinacion(UUID empresaId, List<SeleccionVariante> selecciones) {
		Map<UUID, UUID> mapa = new LinkedHashMap<>();
		for (SeleccionVariante seleccion : selecciones) {
			if (seleccion.tipoEtiquetaId() == null || seleccion.valorEtiquetaId() == null) {
				throw new CombinacionDeVarianteInvalida("Cada selección requiere tipo y valor de etiqueta");
			}
			if (mapa.containsKey(seleccion.tipoEtiquetaId())) {
				throw new CombinacionDeVarianteInvalida("Una variante no puede repetir la misma dimensión de etiqueta");
			}
			if (!taxonomia.tipoDefineVariante(empresaId, seleccion.tipoEtiquetaId())) {
				throw new CombinacionDeVarianteInvalida("El tipo de etiqueta no existe o no define variantes");
			}
			if (!taxonomia.valorPerteneceATipo(empresaId, seleccion.tipoEtiquetaId(), seleccion.valorEtiquetaId())) {
				throw new CombinacionDeVarianteInvalida("El valor no pertenece al tipo de etiqueta indicado");
			}
			mapa.put(seleccion.tipoEtiquetaId(), seleccion.valorEtiquetaId());
		}
		return CombinacionDeVariante.de(mapa);
	}

	private void exigirVarianteNoDuplicada(UUID prendaId, CombinacionDeVariante combinacion) {
		boolean yaExiste = grupos.listarPorPrenda(prendaId).stream()
				.anyMatch(grupo -> grupo.combinacion().equals(combinacion));
		if (yaExiste) {
			throw new VarianteDuplicada();
		}
	}

	private void exigirPrendaDelTenant(UUID empresaId, UUID prendaId) {
		prendas.buscarPorId(prendaId)
				.filter(prenda -> prenda.empresaId().equals(empresaId))
				.orElseThrow(() -> new PrendaNoEncontrada(prendaId));
	}
}
