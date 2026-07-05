package com.costumi.backend.rentas.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Rentas (scoped por tenant). */
public interface RentaRepository {

	Renta guardar(Renta renta);

	Optional<Renta> buscarPorId(UUID id);

	List<Renta> listarPorEmpresa(UUID empresaId);

	List<Renta> listarPorCliente(UUID empresaId, UUID clienteId);
}
