package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.SolicitudDeReembolso;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: bandeja de solicitudes de reembolso de la empresa (RF-4.5/6.9). */
public interface ConsultarReembolsos {

	List<SolicitudDeReembolso> deEmpresa(UUID empresaId);

	/** Página de solicitudes, con búsqueda opcional por motivo. */
	com.costumi.backend.compartido.Pagina<SolicitudDeReembolso> deEmpresa(UUID empresaId, String buscar, com.costumi.backend.compartido.SolicitudDePagina pagina);
}
