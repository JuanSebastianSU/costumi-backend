package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.RegistrarSucursal;
import com.costumi.backend.identidad.aplicacion.RegistrarSucursalComando;
import com.costumi.backend.identidad.dominio.Sucursal;
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

/** Sucursales de una Empresa (recurso anidado bajo /empresas/{empresaId}). */
@RestController
@RequestMapping("/api/v1/empresas/{empresaId}/sucursales")
class SucursalController {

	private final RegistrarSucursal registrarSucursal;

	SucursalController(RegistrarSucursal registrarSucursal) {
		this.registrarSucursal = registrarSucursal;
	}

	/** Alta de una Sucursal para la Empresa (RF-15.1). Requiere empresa ACTIVA (RF-15.4). */
	@PostMapping
	ResponseEntity<SucursalResponse> registrar(@PathVariable UUID empresaId,
			@Valid @RequestBody RegistrarSucursalRequest request, UriComponentsBuilder uriBuilder) {
		Sucursal sucursal = registrarSucursal.ejecutar(
				new RegistrarSucursalComando(empresaId, request.nombre(), request.direccion()));
		URI location = uriBuilder.path("/api/v1/empresas/{empresaId}/sucursales/{id}")
				.buildAndExpand(empresaId, sucursal.id()).toUri();
		return ResponseEntity.created(location).body(SucursalResponse.desde(sucursal));
	}
}
