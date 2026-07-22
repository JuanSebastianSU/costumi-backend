package com.costumi.backend.devoluciones.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Devoluciones con su checklist (scoped por tenant). */
public interface DevolucionRepository {

	Devolucion guardar(Devolucion devolucion);

	Optional<Devolucion> buscarPorId(UUID id);

	List<Devolucion> listarPorEmpresa(UUID empresaId);

	/** Página de devoluciones, con búsqueda opcional en la descripción de las piezas revisadas. */
	com.costumi.backend.compartido.Pagina<Devolucion> listarPorEmpresa(UUID empresaId, String buscar, com.costumi.backend.compartido.SolicitudDePagina pagina);

	/** Devoluciones registradas contra una renta (para acumular lo ya devuelto y detectar la total, RF-5.5). */
	List<Devolucion> listarPorRenta(UUID empresaId, UUID rentaId);
}
