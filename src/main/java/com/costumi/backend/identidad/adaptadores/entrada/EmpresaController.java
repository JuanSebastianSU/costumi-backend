package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.compartido.ContextoDeTenant;
import com.costumi.backend.identidad.aplicacion.ConsultarMiEmpresa;
import com.costumi.backend.identidad.aplicacion.ConsultarEmpresas;
import com.costumi.backend.identidad.aplicacion.ConsultarEmpresasPendientes;
import com.costumi.backend.identidad.aplicacion.GestionarEmpresa;
import com.costumi.backend.identidad.aplicacion.RegistrarEmpresa;
import com.costumi.backend.identidad.aplicacion.RegistrarEmpresaComando;
import com.costumi.backend.identidad.dominio.Empresa;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Puerta de entrada REST del módulo de Identidad/tenant (API /api/v1). */
@RestController
@RequestMapping("/api/v1/empresas")
class EmpresaController {

	private final RegistrarEmpresa registrarEmpresa;
	private final GestionarEmpresa gestionarEmpresa;
	private final ConsultarEmpresasPendientes consultarEmpresasPendientes;
	private final ConsultarEmpresas consultarEmpresas;
	private final ConsultarMiEmpresa consultarMiEmpresa;
	private final ContextoDeTenant tenant;

	EmpresaController(RegistrarEmpresa registrarEmpresa, GestionarEmpresa gestionarEmpresa,
			ConsultarEmpresasPendientes consultarEmpresasPendientes, ConsultarEmpresas consultarEmpresas,
			ConsultarMiEmpresa consultarMiEmpresa, ContextoDeTenant tenant) {
		this.registrarEmpresa = registrarEmpresa;
		this.gestionarEmpresa = gestionarEmpresa;
		this.consultarEmpresasPendientes = consultarEmpresasPendientes;
		this.consultarEmpresas = consultarEmpresas;
		this.consultarMiEmpresa = consultarMiEmpresa;
		this.tenant = tenant;
	}

	/**
	 * La <b>propia</b> tienda del usuario autenticado (RF-15.1): su nombre es lo que la app necesita para
	 * encabezar Gestión, y hasta ahora no había forma de leerlo — el único listado de empresas es del
	 * SuperAdmin. La empresa sale del token, así que no se puede pedir la de otro (§5.4).
	 */
	@GetMapping("/mia")
	EmpresaResponse mia() {
		return EmpresaResponse.desde(consultarMiEmpresa.ejecutar(tenant.empresaIdRequerida()));
	}

	/**
	 * Cola de solicitudes PENDIENTES para el SuperAdmin, con marca de vencidas (RF-15.3/15.4).
	 */
	@GetMapping("/pendientes")
	List<EmpresaPendienteResponse> pendientes() {
		return consultarEmpresasPendientes.ejecutar().stream().map(EmpresaPendienteResponse::desde).toList();
	}

	/**
	 * Listado de Empresas ACTIVAS y SUSPENDIDAS para el SuperAdmin (RF-15.3): la lista desde la que se
	 * suspende o reactiva una empresa.
	 */
	@GetMapping
	List<EmpresaResumenResponse> listar() {
		return consultarEmpresas.ejecutar().stream().map(EmpresaResumenResponse::desde).toList();
	}

	/**
	 * Registro / solicitud de tienda de una Empresa (RF-15.2): nace PENDIENTE, devuelve 201.
	 * Endpoint público; si viene con token (un CLIENTE del marketplace pidiendo abrir su tienda),
	 * se guarda su id como solicitante para que el SuperAdmin sepa a quién promover a Dueño al aprobar.
	 */
	@PostMapping
	ResponseEntity<EmpresaResponse> registrar(@Valid @RequestBody RegistrarEmpresaRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID solicitanteId = (jwt == null) ? null : UUID.fromString(jwt.getSubject());
		Empresa empresa = registrarEmpresa.ejecutar(new RegistrarEmpresaComando(
				request.nombre(), request.ubicacion(), request.contacto(), solicitanteId));
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
