package com.costumi.backend.disfraces.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia del agregado Disfraz (cabecera + slots), scoped por tenant. */
public interface DisfrazRepository {

	Disfraz guardar(Disfraz disfraz);

	Optional<Disfraz> buscarPorId(UUID id);

	List<Disfraz> listarPorEmpresa(UUID empresaId);

	/** Página de disfraces por nombre, filtrando opcionalmente por texto y por categoría. */
	com.costumi.backend.compartido.Pagina<Disfraz> listarPorEmpresa(UUID empresaId, String buscar, UUID categoriaId, com.costumi.backend.compartido.SolicitudDePagina pagina);
}
