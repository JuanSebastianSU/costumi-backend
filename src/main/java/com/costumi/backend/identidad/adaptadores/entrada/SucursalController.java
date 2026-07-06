package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.AccesoAlTenantDenegado;
import com.costumi.backend.identidad.aplicacion.ListarSucursales;
import com.costumi.backend.identidad.aplicacion.RegistrarSucursal;
import com.costumi.backend.identidad.aplicacion.RegistrarSucursalComando;
import com.costumi.backend.identidad.dominio.Sucursal;
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

/** Sucursales de una Empresa (recurso anidado bajo /empresas/{empresaId}). */
@RestController
@RequestMapping("/api/v1/empresas/{empresaId}/sucursales")
class SucursalController {

	private final RegistrarSucursal registrarSucursal;
	private final ListarSucursales listarSucursales;

	SucursalController(RegistrarSucursal registrarSucursal, ListarSucursales listarSucursales) {
		this.registrarSucursal = registrarSucursal;
		this.listarSucursales = listarSucursales;
	}

	/**
	 * Sucursales de la empresa (RF-15.1). Cualquier usuario autenticado de la empresa puede leerlas
	 * (las necesita para operar: rentas, ventas, caja); se valida que el token pertenezca al tenant.
	 */
	@GetMapping
	ResponseEntity<List<SucursalResponse>> listar(@PathVariable UUID empresaId,
			@AuthenticationPrincipal Jwt jwt) {
		verificarDuenoDelTenant(jwt, empresaId);
		List<SucursalResponse> sucursales = listarSucursales.deEmpresa(empresaId).stream()
				.map(SucursalResponse::desde)
				.toList();
		return ResponseEntity.ok(sucursales);
	}

	/**
	 * Alta de una Sucursal (RF-15.1). El rol (DUENO/ENCARGADO) lo exige la config de seguridad;
	 * aquí se valida además que el usuario pertenezca a esa empresa (aislamiento por tenant).
	 */
	@PostMapping
	ResponseEntity<SucursalResponse> registrar(@PathVariable UUID empresaId,
			@Valid @RequestBody RegistrarSucursalRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		verificarDuenoDelTenant(jwt, empresaId);
		Sucursal sucursal = registrarSucursal.ejecutar(
				new RegistrarSucursalComando(empresaId, request.nombre(), request.direccion()));
		URI location = uriBuilder.path("/api/v1/empresas/{empresaId}/sucursales/{id}")
				.buildAndExpand(empresaId, sucursal.id()).toUri();
		return ResponseEntity.created(location).body(SucursalResponse.desde(sucursal));
	}

	private static void verificarDuenoDelTenant(Jwt jwt, UUID empresaId) {
		String empresaDelToken = jwt.getClaimAsString("empresa_id");
		if (empresaDelToken == null || !empresaDelToken.equals(empresaId.toString())) {
			throw new AccesoAlTenantDenegado(empresaId);
		}
	}
}
