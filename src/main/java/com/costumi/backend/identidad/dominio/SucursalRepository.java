package com.costumi.backend.identidad.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Sucursales. Lo implementa un adaptador en salida. */
public interface SucursalRepository {

	Sucursal guardar(Sucursal sucursal);

	Optional<Sucursal> buscarPorId(UUID id);

	/** Sucursales de una empresa (scoped por tenant). */
	List<Sucursal> listarPorEmpresa(UUID empresaId);
}
