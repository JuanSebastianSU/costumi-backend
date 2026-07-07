package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auto-registro de un CLIENTE (usuario final del marketplace): valida el correo único, exige una
 * contraseña mínima, cifra con BCrypt y crea la cuenta sin empresa. Devuelve credenciales (auto-login).
 */
@Service
class RegistrarClienteService implements RegistrarCliente {

	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;
	private final EmisorDeTokens emisor;

	RegistrarClienteService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, EmisorDeTokens emisor) {
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
		this.emisor = emisor;
	}

	@Override
	@Transactional
	public Credenciales ejecutar(String email, String password) {
		if (password == null || password.length() < 8) {
			throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
		}
		String normalizado = email == null ? null : email.trim().toLowerCase();
		if (normalizado != null && usuarios.buscarPorEmail(normalizado).isPresent()) {
			throw new EmailYaRegistrado(normalizado);
		}
		Usuario cliente = usuarios.guardar(
				Usuario.crear(null, normalizado, passwordEncoder.encode(password), Rol.CLIENTE));
		return new Credenciales(emisor.emitir(cliente), emisor.emitirRefresh(cliente));
	}
}
