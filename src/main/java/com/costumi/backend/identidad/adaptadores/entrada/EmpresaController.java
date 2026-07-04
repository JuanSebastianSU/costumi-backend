package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.GestionarEmpresa;
import com.costumi.backend.identidad.aplicacion.RegistrarEmpresa;
import com.costumi.backend.identidad.aplicacion.RegistrarEmpresaComando;
import com.costumi.backend.identidad.dominio.Empresa;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/** Puerta de entrada REST del módulo de Identidad/tenant (API /api/v1). */
@RestController
@RequestMapping("/api/v1/empresas")
class EmpresaController {

	private final RegistrarEmpresa registrarEmpresa;
	private final GestionarEmpresa gestionarEmpresa;

	EmpresaController(RegistrarEmpresa registrarEmpresa, GestionarEmpresa gestionarEmpresa) {
		this.registrarEmpresa = registrarEmpresa;
		this.gestionarEmpresa = gestionarEmpresa;
	}

	/** Auto-registro de una Empresa (RF-15.2): nace PENDIENTE, devuelve 201. */
	@PostMapping
	ResponseEntity<EmpresaResponse> registrar(@Valid @RequestBody RegistrarEmpresaRequest request,
			UriComponentsBuilder uriBuilder) {
		Empresa empresa = registrarEmpresa.ejecutar(new RegistrarEmpresaComando(request.nombre()));
		URI location = uriBuilder.path("/api/v1/empresas/{id}").buildAndExpand(empresa.id()).toUri();
		return ResponseEntity.created(location).body(EmpresaResponse.desde(empresa));
	}

	/** SuperAdmin aprueba una empresa PENDIENTE (RF-15.3). */
	@PostMapping("/{id}/aprobar")
	ResponseEntity<EmpresaResponse> aprobar(@PathVariable UUID id) {
		return ResponseEntity.ok(EmpresaResponse.desde(gestionarEmpresa.aprobar(id)));
	}

	/** SuperAdmin rechaza una empresa PENDIENTE (RF-15.3). */
	@PostMapping("/{id}/rechazar")
	ResponseEntity<EmpresaResponse> rechazar(@PathVariable UUID id) {
		return ResponseEntity.ok(EmpresaResponse.desde(gestionarEmpresa.rechazar(id)));
	}

	/** SuperAdmin suspende una empresa ACTIVA (RF-15.3). */
	@PostMapping("/{id}/suspender")
	ResponseEntity<EmpresaResponse> suspender(@PathVariable UUID id) {
		return ResponseEntity.ok(EmpresaResponse.desde(gestionarEmpresa.suspender(id)));
	}

	/** Reactiva una empresa SUSPENDIDA. */
	@PostMapping("/{id}/reactivar")
	ResponseEntity<EmpresaResponse> reactivar(@PathVariable UUID id) {
		return ResponseEntity.ok(EmpresaResponse.desde(gestionarEmpresa.reactivar(id)));
	}
}
