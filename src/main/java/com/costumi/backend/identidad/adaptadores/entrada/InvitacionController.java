package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.Credenciales;
import com.costumi.backend.identidad.aplicacion.GestionarInvitaciones;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cara pública de una invitación de trabajo (Fase B, paso 3): ver a qué tienda/rol la invitan (desde el
 * enlace) y aceptarla (con T&C). Es público porque la persona puede no tener cuenta todavía; lo autoriza el
 * token de la invitación, no un login.
 */
@RestController
@RequestMapping("/api/v1/invitaciones")
class InvitacionController {

	private final GestionarInvitaciones invitaciones;

	InvitacionController(GestionarInvitaciones invitaciones) {
		this.invitaciones = invitaciones;
	}

	/** Vista de la invitación desde el enlace: tienda, rol, email y si la persona necesita crear cuenta. */
	@GetMapping("/{token}")
	InvitacionVistaResponse ver(@PathVariable String token) {
		GestionarInvitaciones.InvitacionVista v = invitaciones.ver(token);
		return new InvitacionVistaResponse(v.empresaNombre(), v.rol().name(), v.email(), v.necesitaCuenta());
	}

	/**
	 * Acepta la invitación (con T&C). Si la persona no tenía cuenta, la crea con la contraseña dada. Devuelve
	 * una sesión (auto-login como la persona; luego entra a «Trabajar» con el switch de contexto).
	 */
	@PostMapping("/aceptar")
	TokenResponse aceptar(@Valid @RequestBody AceptarInvitacionRequest request) {
		Credenciales cred = invitaciones.aceptar(new GestionarInvitaciones.AceptarComando(
				request.token(), request.password(), request.aceptaTerminos()));
		return new TokenResponse(cred.accessToken(), cred.refreshToken(), "Bearer");
	}

	record InvitacionVistaResponse(String empresaNombre, String rol, String email, boolean necesitaCuenta) {
	}

	/** {@code password} solo hace falta si la persona no tiene cuenta; {@code aceptaTerminos} debe ser true. */
	record AceptarInvitacionRequest(@NotBlank String token, String password, boolean aceptaTerminos) {
	}
}
