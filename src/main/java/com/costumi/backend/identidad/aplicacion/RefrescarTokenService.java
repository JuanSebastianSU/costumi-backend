package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.TokenDeRefresh;
import com.costumi.backend.identidad.dominio.TokenDeRefreshRepository;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Rota el refresco (RF-1.1, C2): valida el token, detecta reuso y emite un nuevo par en la misma familia.
 * También cierra sesión (logout) revocando la familia. La rotación es la clave de la revocación: cada uso
 * consume el refresco (queda {@code ROTADO}) y emite uno nuevo; si vuelve a llegar un refresco ya rotado o
 * revocado, es señal de robo → se anula toda la familia y ninguna copia sirve más.
 */
@Service
class RefrescarTokenService implements RefrescarToken, CerrarSesion {

	private final ValidadorDeRefresh validador;
	private final UsuarioRepository usuarios;
	private final TokenDeRefreshRepository tokens;
	private final EmisorDeSesion sesiones;
	private final RevocadorDeSesion revocador;

	RefrescarTokenService(ValidadorDeRefresh validador, UsuarioRepository usuarios,
			TokenDeRefreshRepository tokens, EmisorDeSesion sesiones, RevocadorDeSesion revocador) {
		this.validador = validador;
		this.usuarios = usuarios;
		this.tokens = tokens;
		this.sesiones = sesiones;
		this.revocador = revocador;
	}

	@Override
	@Transactional
	public Credenciales ejecutar(String refreshToken) {
		RefreshDecodificado datos = validador.validar(refreshToken);
		Usuario usuario = usuarios.buscarPorEmail(datos.email()).orElseThrow(RefreshInvalido::new);
		// Una cuenta dada de baja no puede renovar sesión (RF-8): acota la vida útil del token vigente.
		if (!usuario.activo()) {
			throw new CuentaDesactivada();
		}
		TokenDeRefresh fila = tokens.buscarPorJti(UUID.fromString(datos.jti())).orElseThrow(RefreshInvalido::new);
		// El jti debe pertenecer al usuario del token (defensa en profundidad).
		if (!fila.usuarioId().equals(usuario.id())) {
			throw new RefreshInvalido();
		}
		// Reuso: un refresco ya ROTADO o REVOCADO que se vuelve a presentar es señal de robo.
		// Se anula toda la familia (incluida la copia legítima que el atacante podría estar por usar).
		// La revocación va en transacción propia (REQUIRES_NEW) para persistir pese al rollback del throw.
		if (!fila.esActivo()) {
			revocador.revocarFamilia(fila.familiaId());
			throw new RefreshInvalido();
		}
		fila.marcarRotado();
		tokens.guardar(fila);
		return sesiones.rotarEnFamilia(usuario, fila.familiaId());
	}

	/** Logout (C2): revoca la familia del refresco. Idempotente y sin filtrar información. */
	@Override
	@Transactional
	public void cerrar(String refreshToken) {
		try {
			RefreshDecodificado datos = validador.validar(refreshToken);
			tokens.buscarPorJti(UUID.fromString(datos.jti()))
					.ifPresent(fila -> tokens.revocarFamilia(fila.familiaId()));
		} catch (RefreshInvalido | IllegalArgumentException ignorado) {
			// token inválido/vencido/jti no-UUID: no hay sesión que cerrar, no se revela nada
		}
	}
}
