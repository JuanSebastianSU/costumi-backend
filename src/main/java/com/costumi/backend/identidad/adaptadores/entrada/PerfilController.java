package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.GestionarPerfilPropio;
import com.costumi.backend.identidad.dominio.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * La propia cuenta (RF-14): cualquier usuario autenticado —cliente, empleado o dueño— ve y edita sus
 * datos y cambia su contraseña, sin que nadie se los tenga que administrar.
 *
 * <p>Siempre opera sobre el usuario del token: no recibe id, así que nadie puede editar la cuenta de otro.
 */
@RestController
@RequestMapping("/api/v1/perfil")
class PerfilController {

	private final GestionarPerfilPropio perfil;

	PerfilController(GestionarPerfilPropio perfil) {
		this.perfil = perfil;
	}

	@GetMapping
	PerfilResponse ver(@AuthenticationPrincipal Jwt jwt) {
		return PerfilResponse.desde(perfil.verPerfil(usuarioDe(jwt)));
	}

	/** Actualiza nombre y teléfono. Vacío borra el dato: son opcionales. El correo no se cambia acá. */
	@PutMapping
	PerfilResponse actualizar(@Valid @RequestBody ActualizarPerfilRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return PerfilResponse.desde(
				perfil.actualizarPerfil(usuarioDe(jwt), request.nombre(), request.telefono()));
	}

	/** Cambia la contraseña dentro de la sesión; exige la actual. */
	@PostMapping("/contrasena")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void cambiarContrasena(@Valid @RequestBody CambiarContrasenaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		perfil.cambiarContrasena(usuarioDe(jwt), request.contrasenaActual(), request.contrasenaNueva());
	}

	private static UUID usuarioDe(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	/** Datos de la cuenta. {@code nombreParaMostrar} evita que la UI tenga que decidir el respaldo. */
	record PerfilResponse(UUID id, String email, String nombre, String telefono, String nombreParaMostrar,
			String rol, UUID empresaId) {

		static PerfilResponse desde(Usuario u) {
			return new PerfilResponse(u.id(), u.email(), u.nombre(), u.telefono(), u.nombreParaMostrar(),
					u.rol().name(), u.empresaId());
		}
	}

	record ActualizarPerfilRequest(@Size(max = 120) String nombre, @Size(max = 40) String telefono) {
	}

	record CambiarContrasenaRequest(@NotBlank String contrasenaActual,
			@NotBlank @Size(min = 8, max = 100) String contrasenaNueva) {
	}
}
