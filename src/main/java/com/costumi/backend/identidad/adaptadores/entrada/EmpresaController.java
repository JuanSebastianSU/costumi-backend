package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.RegistrarEmpresa;
import com.costumi.backend.identidad.aplicacion.RegistrarEmpresaComando;
import com.costumi.backend.identidad.dominio.Empresa;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/** Puerta de entrada REST del módulo de Identidad/tenant (API /api/v1). */
@RestController
@RequestMapping("/api/v1/empresas")
class EmpresaController {

	private final RegistrarEmpresa registrarEmpresa;

	EmpresaController(RegistrarEmpresa registrarEmpresa) {
		this.registrarEmpresa = registrarEmpresa;
	}

	/** Auto-registro de una Empresa (RF-15.2): nace PENDIENTE, devuelve 201. */
	@PostMapping
	ResponseEntity<EmpresaResponse> registrar(@Valid @RequestBody RegistrarEmpresaRequest request,
			UriComponentsBuilder uriBuilder) {
		Empresa empresa = registrarEmpresa.ejecutar(new RegistrarEmpresaComando(request.nombre()));
		URI location = uriBuilder.path("/api/v1/empresas/{id}").buildAndExpand(empresa.id()).toUri();
		return ResponseEntity.created(location).body(EmpresaResponse.desde(empresa));
	}
}
