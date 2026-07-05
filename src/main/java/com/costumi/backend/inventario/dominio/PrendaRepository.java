package com.costumi.backend.inventario.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Prendas (scoped por tenant). */
public interface PrendaRepository {

	Prenda guardar(Prenda prenda);

	Optional<Prenda> buscarPorId(UUID id);

	List<Prenda> listarPorEmpresa(UUID empresaId);
}
