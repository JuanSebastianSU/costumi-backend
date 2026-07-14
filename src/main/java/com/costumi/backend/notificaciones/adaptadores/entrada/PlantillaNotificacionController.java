package com.costumi.backend.notificaciones.adaptadores.entrada;

import com.costumi.backend.notificaciones.aplicacion.GestionarPlantillas;
import com.costumi.backend.notificaciones.dominio.TipoDeEvento;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Plantillas de mensajes automáticos de la empresa (RF-11), acotadas al tenant del token. */
@RestController
@RequestMapping("/api/v1/notificaciones/plantillas")
class PlantillaNotificacionController {

	private final GestionarPlantillas plantillas;

	PlantillaNotificacionController(GestionarPlantillas plantillas) {
		this.plantillas = plantillas;
	}

	@GetMapping
	List<PlantillaResponse> listar(@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return plantillas.deEmpresa(empresaId).stream().map(PlantillaResponse::desde).toList();
	}

	@PutMapping("/{tipo}")
	PlantillaResponse actualizar(@PathVariable String tipo, @Valid @RequestBody ActualizarPlantillaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return PlantillaResponse.desde(
				plantillas.actualizar(empresaId, tipoValido(tipo), request.texto(), request.activa()));
	}

	private static TipoDeEvento tipoValido(String tipo) {
		try {
			return TipoDeEvento.valueOf(tipo);
		}
		catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de evento desconocido: " + tipo);
		}
	}
}
