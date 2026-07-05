package com.costumi.backend.configuracion.adaptadores.entrada;

import com.costumi.backend.configuracion.aplicacion.ActualizarConfiguracionComando;
import com.costumi.backend.configuracion.aplicacion.GestionarConfiguracion;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Configuración de la empresa (RF-12), acotada al tenant del token. */
@RestController
@RequestMapping("/api/v1/configuracion")
class ConfiguracionController {

	private final GestionarConfiguracion gestionarConfiguracion;

	ConfiguracionController(GestionarConfiguracion gestionarConfiguracion) {
		this.gestionarConfiguracion = gestionarConfiguracion;
	}

	@GetMapping
	ConfiguracionResponse obtener(@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return ConfiguracionResponse.desde(gestionarConfiguracion.deEmpresa(empresaId));
	}

	@PutMapping
	ConfiguracionResponse actualizar(@RequestBody ConfiguracionRequest request, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return ConfiguracionResponse.desde(gestionarConfiguracion.actualizar(new ActualizarConfiguracionComando(
				empresaId, request.conteoStock(), request.multasActivo(), request.multiSucursal(),
				request.pagoEnLinea())));
	}
}
