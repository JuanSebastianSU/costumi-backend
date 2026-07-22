package com.costumi.backend.auditoria.adaptadores.entrada;

import com.costumi.backend.auditoria.aplicacion.ConsultarAuditoria;
import com.costumi.backend.compartido.ContextoDeTenant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Auditoría (RF-0.5/15.5): consulta del trail de la empresa, acotado al tenant. Solo lectura. */
@RestController
@RequestMapping("/api/v1/auditoria")
class AuditoriaController {

	private final ConsultarAuditoria consultarAuditoria;
	private final ContextoDeTenant tenant;

	AuditoriaController(ConsultarAuditoria consultarAuditoria, ContextoDeTenant tenant) {
		this.consultarAuditoria = consultarAuditoria;
		this.tenant = tenant;
	}

	/**
	 * Página del trail, más reciente primero. {@code buscar} filtra por acción o detalle. Se pagina
	 * siempre: la auditoría crece con cada acción y devolverla entera deja de funcionar con el tiempo.
	 */
	@GetMapping
	com.costumi.backend.compartido.RespuestaPaginada<AuditoriaResponse> listar(
			@org.springframework.web.bind.annotation.RequestParam(required = false) String buscar,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Integer pagina,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Integer tamano) {
		return tenant.empresaId()
				.map(empresaId -> com.costumi.backend.compartido.RespuestaPaginada.desde(
						consultarAuditoria.deEmpresa(empresaId, buscar,
								com.costumi.backend.compartido.SolicitudDePagina.de(pagina, tamano)),
						AuditoriaResponse::desde))
				.orElseGet(() -> new com.costumi.backend.compartido.RespuestaPaginada<>(List.of(), 0, 0, 0, 0));
	}
}
