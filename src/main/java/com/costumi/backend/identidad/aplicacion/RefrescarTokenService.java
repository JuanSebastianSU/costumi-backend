package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Valida el refresh, ubica al usuario y emite un nuevo par de tokens (rotación), RF-1.1. */
@Service
class RefrescarTokenService implements RefrescarToken {

	private final ValidadorDeRefresh validador;
	private final UsuarioRepository usuarios;
	private final EmisorDeTokens emisor;

	RefrescarTokenService(ValidadorDeRefresh validador, UsuarioRepository usuarios, EmisorDeTokens emisor) {
		this.validador = validador;
		this.usuarios = usuarios;
		this.emisor = emisor;
	}

	@Override
	@Transactional(readOnly = true)
	public Credenciales ejecutar(String refreshToken) {
		String email = validador.emailDelRefresh(refreshToken);
		Usuario usuario = usuarios.buscarPorEmail(email).orElseThrow(RefreshInvalido::new);
		// Una cuenta dada de baja no puede renovar sesión (RF-8): acota la vida útil del token vigente.
		if (!usuario.activo()) {
			throw new CuentaDesactivada();
		}
		return new Credenciales(emisor.emitir(usuario), emisor.emitirRefresh(usuario));
	}
}
