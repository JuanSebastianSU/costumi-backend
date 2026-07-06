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

	@GetMapping
	List<AuditoriaResponse> listar() {
		return tenant.empresaId()
				.map(empresaId -> consultarAuditoria.deEmpresa(empresaId).stream().map(AuditoriaResponse::desde).toList())
				.orElseGet(List::of);
	}
}
