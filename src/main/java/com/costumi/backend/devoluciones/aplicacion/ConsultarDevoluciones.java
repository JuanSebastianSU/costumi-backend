package com.costumi.backend.devoluciones.aplicacion;

import com.costumi.backend.devoluciones.dominio.Devolucion;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista las Devoluciones de una empresa (scoped por tenant). */
public interface ConsultarDevoluciones {

	List<Devolucion> deEmpresa(UUID empresaId);

	/** Página de devoluciones, con búsqueda opcional en la descripción de las piezas revisadas. */
	com.costumi.backend.compartido.Pagina<Devolucion> deEmpresa(UUID empresaId, String buscar, com.costumi.backend.compartido.SolicitudDePagina pagina);
}
