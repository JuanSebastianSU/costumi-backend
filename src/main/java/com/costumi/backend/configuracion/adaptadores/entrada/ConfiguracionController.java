package com.costumi.backend.configuracion.adaptadores.entrada;

import com.costumi.backend.configuracion.aplicacion.ActualizarConfiguracionComando;
import com.costumi.backend.configuracion.aplicacion.GestionarConfiguracion;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
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
		return ConfiguracionResponse.desde(gestionarConfiguracion.actualizar(aComando(empresaId, request)));
	}

	/** Respaldo de la configuración (RF-12.3): igual que GET, pensado para exportar/guardar. */
	@GetMapping("/export")
	ConfiguracionResponse exportar(@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return ConfiguracionResponse.desde(gestionarConfiguracion.deEmpresa(empresaId));
	}

	/** Restauración de la configuración (RF-12.3): aplica un respaldo previamente exportado. */
	@PostMapping("/import")
	ConfiguracionResponse importar(@RequestBody ConfiguracionRequest request, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return ConfiguracionResponse.desde(gestionarConfiguracion.actualizar(aComando(empresaId, request)));
	}

	private static ActualizarConfiguracionComando aComando(UUID empresaId, ConfiguracionRequest request) {
		BigDecimal tasa = request.tasaImpuesto() == null ? BigDecimal.ZERO : request.tasaImpuesto();
		String moneda = (request.moneda() == null || request.moneda().isBlank()) ? "COP" : request.moneda();
		BigDecimal recargo = request.recargoPorRetrasoPorDia() == null ? BigDecimal.ZERO
				: request.recargoPorRetrasoPorDia();
		return new ActualizarConfiguracionComando(empresaId, request.conteoStock(), request.multasActivo(),
				request.multiSucursal(), request.pagoEnLinea(), tasa, moneda, recargo);
	}
}
