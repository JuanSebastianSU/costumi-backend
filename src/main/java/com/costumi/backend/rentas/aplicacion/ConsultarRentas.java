package com.costumi.backend.rentas.aplicacion;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;
import com.costumi.backend.rentas.dominio.Renta;
import com.costumi.backend.rentas.dominio.ResumenDeRentas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de entrada: lista Rentas de una empresa, opcionalmente por cliente (scoped por tenant). */
public interface ConsultarRentas {

	List<Renta> buscar(UUID empresaId, UUID clienteId);

	/**
	 * Página de rentas de la empresa, opcionalmente por cliente (C3). {@code buscar} filtra por código de
	 * retiro; {@code filtro} es la bandeja del ciclo (POR_ENTREGAR/ACTIVAS/VENCIDAS/CERRADAS; nulo = todas).
	 */
	Pagina<Renta> listar(UUID empresaId, UUID clienteId, String buscar, String filtro, SolicitudDePagina solicitud);

	/** Conteo de rentas por bandeja, para las pestañas de G9. */
	ResumenDeRentas resumen(UUID empresaId);

	/** Una renta de la empresa por su id (scoped por tenant). Vacío si no existe o es de otra empresa. */
	Optional<Renta> buscarPorId(UUID empresaId, UUID rentaId);
}
