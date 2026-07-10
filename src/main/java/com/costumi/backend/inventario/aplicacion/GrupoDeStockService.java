package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.catalogo.ConsultaDeTaxonomia;
import com.costumi.backend.identidad.ConsultaDeSucursales;
import com.costumi.backend.inventario.StockAjustado;
import com.costumi.backend.inventario.dominio.CombinacionDeVariante;
import com.costumi.backend.inventario.dominio.GrupoDeStock;
import com.costumi.backend.inventario.dominio.GrupoDeStockRepository;
import com.costumi.backend.inventario.dominio.Prenda;
import com.costumi.backend.inventario.dominio.PrendaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Casos de uso de Grupos de stock (variantes), acotados a la empresa (tenant). */
@Service
class GrupoDeStockService implements CrearGrupoDeStock, ConsultarGruposDeStock, MoverUnidades, ReabastecerGrupo,
		ConsultarStockBajo, AjustarStock, TransferirStock {

	private final PrendaRepository prendas;
	private final GrupoDeStockRepository grupos;
	private final ConsultaDeTaxonomia taxonomia;
	private final ConsultaDeSucursales sucursales;
	private final ApplicationEventPublisher eventos;

	GrupoDeStockService(PrendaRepository prendas, GrupoDeStockRepository grupos, ConsultaDeTaxonomia taxonomia,
			ConsultaDeSucursales sucursales, ApplicationEventPublisher eventos) {
		this.prendas = prendas;
		this.grupos = grupos;
		this.taxonomia = taxonomia;
		this.sucursales = sucursales;
		this.eventos = eventos;
	}

	@Override
	@Transactional
	public GrupoDeStock ejecutar(CrearGrupoDeStockComando comando) {
		Prenda prenda = exigirPrendaDelTenant(comando.empresaId(), comando.prendaId());
		exigirSucursalActiva(comando.empresaId(), comando.sucursalId());
		CombinacionDeVariante combinacion = validarCombinacion(comando.empresaId(), prenda.categoriaId(),
				comando.combinacion());
		exigirVarianteNoDuplicada(comando.prendaId(), comando.sucursalId(), combinacion);
		return grupos.guardar(GrupoDeStock.crear(
				comando.empresaId(), comando.sucursalId(), comando.prendaId(), combinacion, comando.cantidadInicial()));
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

	@Override
	@Transactional
	public GrupoDeStock ejecutar(UUID empresaId, UUID grupoId, int cantidad) {
		GrupoDeStock grupo = grupos.buscarPorId(grupoId)
				.filter(g -> g.empresaId().equals(empresaId))
				.orElseThrow(() -> new GrupoDeStockNoEncontrado(grupoId));
		grupo.reabastecer(cantidad);
		return grupos.guardar(grupo);
	}

	@Override
	@Transactional(readOnly = true)
	public List<GrupoDeStock> deEmpresa(UUID empresaId, int umbral) {
		return grupos.listarBajoUmbral(empresaId, umbral);
	}

	@Override
	@Transactional
	public GrupoDeStock ejecutar(AjustarStockComando comando) {
		GrupoDeStock grupo = grupos.buscarPorId(comando.grupoId())
				.filter(g -> g.empresaId().equals(comando.empresaId()))
				.orElseThrow(() -> new GrupoDeStockNoEncontrado(comando.grupoId()));
		grupo.ajustar(comando.estado(), comando.delta());
		GrupoDeStock guardado = grupos.guardar(grupo);
		// Traza del ajuste (RF-10): Auditoría lo registra vía el evento (§5.5).
		eventos.publishEvent(new StockAjustado(comando.empresaId(), guardado.prendaId(), guardado.id(),
				comando.estado().name(), comando.delta(), comando.motivo()));
		return guardado;
	}

	@Override
	@Transactional
	public GrupoDeStock ejecutar(TransferirStockComando comando) {
		GrupoDeStock origen = grupos.buscarPorId(comando.grupoOrigenId())
				.filter(g -> g.empresaId().equals(comando.empresaId()))
				.orElseThrow(() -> new GrupoDeStockNoEncontrado(comando.grupoOrigenId()));
		if (comando.sucursalDestinoId() == null || comando.sucursalDestinoId().equals(origen.sucursalId())) {
			throw new IllegalArgumentException("La sucursal de destino debe ser distinta a la de origen");
		}
		exigirSucursalActiva(comando.empresaId(), comando.sucursalDestinoId());
		// Destino = mismo grupo (prenda + variante) en la sucursal de destino; si no existe, se crea vacío.
		GrupoDeStock destino = grupos.listarPorPrendaYSucursal(origen.prendaId(), comando.sucursalDestinoId()).stream()
				.filter(origen::mismaVariante)
				.findFirst()
				.orElseGet(() -> GrupoDeStock.crear(comando.empresaId(), comando.sucursalDestinoId(), origen.prendaId(),
						origen.combinacion(), 0));
		// Las unidades salen del origen y entran al destino (el dominio valida la disponibilidad).
		origen.darDeBaja(comando.cantidad());
		destino.reabastecer(comando.cantidad());
		GrupoDeStock origenGuardado = grupos.guardar(origen);
		GrupoDeStock destinoGuardado = grupos.guardar(destino);
		// Traza del movimiento (RF-10/RF-0.5): Auditoría reacciona al evento (§5.5).
		eventos.publishEvent(new StockAjustado(comando.empresaId(), origen.prendaId(), destinoGuardado.id(),
				"DISPONIBLE", comando.cantidad(), "transferencia entre sucursales"));
		return origenGuardado;
	}

	/**
	 * Convierte las selecciones en una {@link CombinacionDeVariante} <b>real</b>: cada dimensión debe
	 * ser un tipo que define variante en la empresa y cada valor debe pertenecer a ese tipo; una
	 * dimensión no se puede repetir. Una lista vacía es la variante única.
	 */
	private CombinacionDeVariante validarCombinacion(UUID empresaId, UUID categoriaId,
			List<SeleccionVariante> selecciones) {
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
			if (!taxonomia.tipoAplicaACategoria(empresaId, seleccion.tipoEtiquetaId(), categoriaId)) {
				throw new CombinacionDeVarianteInvalida("El tipo de etiqueta no aplica a la categoría de la prenda");
			}
			if (!taxonomia.valorPerteneceATipo(empresaId, seleccion.tipoEtiquetaId(), seleccion.valorEtiquetaId())) {
				throw new CombinacionDeVarianteInvalida("El valor no pertenece al tipo de etiqueta indicado");
			}
			mapa.put(seleccion.tipoEtiquetaId(), seleccion.valorEtiquetaId());
		}
		return CombinacionDeVariante.de(mapa);
	}

	/** SEC-1: la sucursal debe existir, ser del tenant y estar activa; si no, referencia colgante. */
	private void exigirSucursalActiva(UUID empresaId, UUID sucursalId) {
		if (!sucursales.existeActiva(empresaId, sucursalId)) {
			throw new IllegalArgumentException("La sucursal no existe o está archivada en esta empresa");
		}
	}

	private void exigirVarianteNoDuplicada(UUID prendaId, UUID sucursalId, CombinacionDeVariante combinacion) {
		// La misma variante puede existir en distintas sucursales; el duplicado se controla por sucursal.
		boolean yaExiste = grupos.listarPorPrendaYSucursal(prendaId, sucursalId).stream()
				.anyMatch(grupo -> grupo.combinacion().equals(combinacion));
		if (yaExiste) {
			throw new VarianteDuplicada();
		}
	}

	private Prenda exigirPrendaDelTenant(UUID empresaId, UUID prendaId) {
		return prendas.buscarPorId(prendaId)
				.filter(prenda -> prenda.empresaId().equals(empresaId))
				.orElseThrow(() -> new PrendaNoEncontrada(prendaId));
	}
}
