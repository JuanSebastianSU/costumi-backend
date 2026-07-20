package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.inventario.dominio.GrupoDeStockRepository;
import com.costumi.backend.inventario.dominio.Prenda;
import com.costumi.backend.inventario.dominio.PrendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
	public boolean prendaExiste(UUID empresaId, UUID prendaId) {
		return prendas.buscarPorId(prendaId)
				.filter(prenda -> prenda.empresaId().equals(empresaId))
				.isPresent();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean prendaTieneStockDisponible(UUID empresaId, UUID prendaId) {
		return prendas.buscarPorId(prendaId)
				.filter(prenda -> prenda.empresaId().equals(empresaId))
				.filter(prenda -> !prenda.archivada())
				.map(prenda -> tieneStock(prenda.id()))
				.orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean poolTieneStockDisponible(UUID empresaId, UUID categoriaId, Map<UUID, Set<UUID>> etiquetasPermitidas) {
		return prendas.listarPorEmpresa(empresaId).stream()
				.filter(prenda -> !prenda.archivada())
				.filter(prenda -> prenda.categoriaId().equals(categoriaId))
				.filter(prenda -> cumpleEtiquetas(prenda, etiquetasPermitidas))
				.anyMatch(prenda -> tieneStock(prenda.id()));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean prendaEnPool(UUID empresaId, UUID prendaId, UUID categoriaId,
			Map<UUID, Set<UUID>> etiquetasPermitidas) {
		return prendas.buscarPorId(prendaId)
				.filter(prenda -> prenda.empresaId().equals(empresaId))
				.filter(prenda -> !prenda.archivada())
				.filter(prenda -> prenda.categoriaId().equals(categoriaId))
				.filter(prenda -> cumpleEtiquetas(prenda, etiquetasPermitidas))
				.isPresent();
	}

	@Override
	@Transactional(readOnly = true)
	public int unidadesDisponibles(UUID empresaId, UUID sucursalId, UUID prendaId) {
		if (!prendaExiste(empresaId, prendaId)) {
			return 0;
		}
		return grupos.listarPorPrendaYSucursal(prendaId, sucursalId).stream()
				.mapToInt(grupo -> grupo.disponibles()).sum();
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Optional<java.math.BigDecimal> precioVenta(UUID empresaId, UUID prendaId) {
		return prendas.buscarPorId(prendaId)
				.filter(prenda -> prenda.empresaId().equals(empresaId))
				.map(com.costumi.backend.inventario.dominio.Prenda::precioVenta);
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Optional<java.math.BigDecimal> precioRenta(UUID empresaId, UUID prendaId) {
		return prendas.buscarPorId(prendaId)
				.filter(prenda -> prenda.empresaId().equals(empresaId))
				.map(com.costumi.backend.inventario.dominio.Prenda::precioRenta);
	}

	@Override
	@Transactional(readOnly = true)
	public List<OpcionDePool> opcionesDelPool(UUID empresaId, UUID categoriaId,
			Map<UUID, Set<UUID>> etiquetasPermitidas) {
		return prendas.listarPorEmpresa(empresaId).stream()
				.filter(prenda -> !prenda.archivada())
				.filter(prenda -> prenda.categoriaId().equals(categoriaId))
				.filter(prenda -> cumpleEtiquetas(prenda, etiquetasPermitidas))
				.map(this::aOpcion)
				.filter(opcion -> opcion.unidadesDisponibles() > 0)
				.sorted(Comparator.comparing(OpcionDePool::nombre, String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<OpcionDePool> opcionDePrenda(UUID empresaId, UUID prendaId) {
		return prendas.buscarPorId(prendaId)
				.filter(prenda -> prenda.empresaId().equals(empresaId))
				.map(this::aOpcion);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, ResumenDePrenda> resumenDePrendas(UUID empresaId, java.util.Collection<UUID> prendaIds) {
		Map<UUID, ResumenDePrenda> resumen = new java.util.LinkedHashMap<>();
		for (UUID prendaId : prendaIds) {
			if (prendaId == null || resumen.containsKey(prendaId)) {
				continue;
			}
			prendas.buscarPorId(prendaId)
					.filter(prenda -> prenda.empresaId().equals(empresaId))
					.ifPresent(prenda -> resumen.put(prenda.id(),
							new ResumenDePrenda(prenda.id(), prenda.nombre(), prenda.fotoUrl())));
		}
		return resumen;
	}

	@Override
	@Transactional(readOnly = true)
	public int contarPrendasEnCategoria(UUID empresaId, UUID categoriaId) {
		return (int) prendas.listarPorEmpresa(empresaId).stream()
				.filter(prenda -> !prenda.archivada())
				.filter(prenda -> prenda.categoriaId().equals(categoriaId))
				.count();
	}

	@Override
	@Transactional(readOnly = true)
	public int contarPrendasConTipoEtiqueta(UUID empresaId, UUID tipoEtiquetaId) {
		return (int) prendas.listarPorEmpresa(empresaId).stream()
				.filter(prenda -> !prenda.archivada())
				.filter(prenda -> prenda.etiquetas().tipos().contains(tipoEtiquetaId))
				.count();
	}

	@Override
	@Transactional(readOnly = true)
	public int contarPrendasConValorEtiqueta(UUID empresaId, UUID valorEtiquetaId) {
		return (int) prendas.listarPorEmpresa(empresaId).stream()
				.filter(prenda -> !prenda.archivada())
				.filter(prenda -> prenda.etiquetas().valores().containsValue(valorEtiquetaId))
				.count();
	}

	@Override
	@Transactional(readOnly = true)
	public int contarUnidadesEnSucursal(UUID empresaId, UUID sucursalId) {
		return grupos.listarPorSucursal(empresaId, sucursalId).stream()
				.mapToInt(grupo -> grupo.total())
				.sum();
	}

	private OpcionDePool aOpcion(Prenda prenda) {
		return new OpcionDePool(prenda.id(), prenda.nombre(), prenda.precioRenta(),
				unidadesDisponiblesTotales(prenda.id()), Map.copyOf(prenda.etiquetas().valores()));
	}

	private int unidadesDisponiblesTotales(UUID prendaId) {
		return grupos.listarPorPrenda(prendaId).stream().mapToInt(grupo -> grupo.disponibles()).sum();
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
