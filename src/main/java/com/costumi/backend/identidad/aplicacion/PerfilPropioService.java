package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.compartido.AlmacenDeImagenesPublico;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Casos de uso de la propia cuenta (RF-14): ver/editar el perfil, la foto y cambiar la contraseña. */
@Service
class PerfilPropioService implements GestionarPerfilPropio {

	/** Mismo mínimo que al restablecer por correo: la regla no puede ser más floja por otra puerta. */
	private static final int LARGO_MINIMO_CONTRASENA = 8;

	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;
	private final AlmacenDeImagenesPublico imagenes;

	PerfilPropioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
			AlmacenDeImagenesPublico imagenes) {
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
		this.imagenes = imagenes;
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
	public Usuario asignarFoto(UUID usuarioId, byte[] contenido) {
		Usuario usuario = exigirUsuario(usuarioId);
		String url = imagenes.subir(contenido, "usuarios/" + usuarioId + "/perfil/");
		return usuarios.guardar(usuario.asignarFoto(url));
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
