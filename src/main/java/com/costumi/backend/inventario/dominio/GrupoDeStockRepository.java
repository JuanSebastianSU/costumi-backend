package com.costumi.backend.inventario.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Grupos de stock (scoped por tenant). */
public interface GrupoDeStockRepository {

	GrupoDeStock guardar(GrupoDeStock grupoDeStock);

	Optional<GrupoDeStock> buscarPorId(UUID id);

	List<GrupoDeStock> listarPorPrenda(UUID prendaId);

	/** Grupos de una prenda en una sucursal concreta (disponibilidad por local, RF-18.2). */
	List<GrupoDeStock> listarPorPrendaYSucursal(UUID prendaId, UUID sucursalId);

	/** Grupos de stock de una sucursal (para saber si un local tiene inventario antes de archivarlo, RF-15.1). */
	List<GrupoDeStock> listarPorSucursal(UUID empresaId, UUID sucursalId);

	/** Grupos de la empresa con disponibles por debajo del umbral (alerta de stock bajo, RF-10). */
	List<GrupoDeStock> listarBajoUmbral(UUID empresaId, int umbral);
}
