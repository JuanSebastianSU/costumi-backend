package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Verifica email + contraseña (BCrypt) y, si son válidas, emite un token de acceso. */
@Service
class AutenticarUsuarioService implements AutenticarUsuario {

	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;
	private final EmisorDeTokens emisor;
	private final String hashDummy;

	AutenticarUsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, EmisorDeTokens emisor) {
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
		this.emisor = emisor;
		// Hash de referencia para igualar el tiempo cuando el email no existe
		// (evita enumeración de usuarios por timing).
		this.hashDummy = passwordEncoder.encode("usuario-inexistente");
	}

	@Override
	@Transactional(readOnly = true)
	public Credenciales autenticar(String email, String password) {
		Usuario usuario = usuarios.buscarPorEmail(email).orElse(null);
		if (usuario == null) {
			passwordEncoder.matches(password, hashDummy); // corre BCrypt igual: no filtra existencia
			throw new CredencialesInvalidas();
		}
		if (!passwordEncoder.matches(password, usuario.passwordHash())) {
			throw new CredencialesInvalidas();
		}
		// La credencial es correcta, pero una cuenta dada de baja no puede iniciar sesión (RF-8).
		if (!usuario.activo()) {
			throw new CuentaDesactivada();
		}
		return new Credenciales(emisor.emitir(usuario), emisor.emitirRefresh(usuario));
	}
}
