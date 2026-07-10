package com.costumi.backend.ventas.aplicacion;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;
import com.costumi.backend.ventas.dominio.Venta;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista las Ventas de una empresa (scoped por tenant). */
public interface ConsultarVentas {

	List<Venta> deEmpresa(UUID empresaId);

	/** Página de ventas de la empresa, más recientes primero (C3). */
	Pagina<Venta> listar(UUID empresaId, SolicitudDePagina solicitud);
}
