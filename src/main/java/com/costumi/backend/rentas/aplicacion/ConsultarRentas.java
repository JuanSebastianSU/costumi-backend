package com.costumi.backend.rentas.aplicacion;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;
import com.costumi.backend.rentas.dominio.Renta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de entrada: lista Rentas de una empresa, opcionalmente por cliente (scoped por tenant). */
public interface ConsultarRentas {

	List<Renta> buscar(UUID empresaId, UUID clienteId);

	/** Página de rentas de la empresa, opcionalmente por cliente (C3). */
	Pagina<Renta> listar(UUID empresaId, UUID clienteId, SolicitudDePagina solicitud);

	/** Una renta de la empresa por su id (scoped por tenant). Vacío si no existe o es de otra empresa. */
	Optional<Renta> buscarPorId(UUID empresaId, UUID rentaId);
}
