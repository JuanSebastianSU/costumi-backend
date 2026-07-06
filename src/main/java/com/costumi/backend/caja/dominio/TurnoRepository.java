package com.costumi.backend.caja.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia del agregado Turno (cabecera + movimientos), scoped por tenant. */
public interface TurnoRepository {

	Turno guardar(Turno turno);

	Optional<Turno> buscarPorId(UUID id);

	List<Turno> listarPorEmpresa(UUID empresaId);
}
