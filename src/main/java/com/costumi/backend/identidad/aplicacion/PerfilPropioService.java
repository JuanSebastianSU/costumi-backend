package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Casos de uso de la propia cuenta (RF-14): ver/editar el perfil y cambiar la contraseña. */
@Service
class PerfilPropioService implements GestionarPerfilPropio {

	/** Mismo mínimo que al restablecer por correo: la regla no puede ser más floja por otra puerta. */
	private static final int LARGO_MINIMO_CONTRASENA = 8;

	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;

	PerfilPropioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional(readOnly = true)
	public Usuario verPerfil(UUID usuarioId) {
		return exigirUsuario(usuarioId);
	}

	@Override
	@Transactional
	public Usuario actualizarPerfil(UUID usuarioId, String nombre, String telefono) {
		return usuarios.guardar(exigirUsuario(usuarioId).actualizarPerfil(nombre, telefono));
	}

	@Override
	@Transactional
	public void cambiarContrasena(UUID usuarioId, String contrasenaActual, String contrasenaNueva) {
		Usuario usuario = exigirUsuario(usuarioId);
		// Se exige la actual aunque la sesión esté abierta: si no, cualquiera que agarre el teléfono
		// desbloqueado podría cambiar la clave y dejar al dueño fuera de su cuenta.
		if (contrasenaActual == null || !passwordEncoder.matches(contrasenaActual, usuario.passwordHash())) {
			throw new CredencialesInvalidas();
		}
		if (contrasenaNueva == null || contrasenaNueva.length() < LARGO_MINIMO_CONTRASENA) {
			throw new IllegalArgumentException(
					"La contraseña nueva debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres");
		}
		usuarios.guardar(usuario.cambiarContrasena(passwordEncoder.encode(contrasenaNueva)));
	}

	private Usuario exigirUsuario(UUID usuarioId) {
		return usuarios.buscarPorId(usuarioId)
				.orElseThrow(() -> new IllegalArgumentException("La cuenta no existe"));
	}
}
