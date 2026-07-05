package com.costumi.backend.ventas.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Ventas con sus líneas (scoped por tenant). */
public interface VentaRepository {

	Venta guardar(Venta venta);

	Optional<Venta> buscarPorId(UUID id);

	List<Venta> listarPorEmpresa(UUID empresaId);
}
