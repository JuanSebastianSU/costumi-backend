package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Da de alta un empleado de la empresa (RF-8): valida el correo único, cifra la contraseña y crea el
 * usuario con su rol. No permite crear SUPERADMIN (es el administrador de la plataforma, no de una empresa).
 */
@Service
class AltaDeEmpleadoService implements AltaDeEmpleado {

	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;

	AltaDeEmpleadoService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public Usuario ejecutar(AltaDeEmpleadoComando comando) {
		if (comando.rol() == Rol.SUPERADMIN) {
			throw new IllegalArgumentException("No se puede crear un SUPERADMIN como empleado de una empresa");
		}
		if (comando.password() == null || comando.password().length() < 8) {
			throw new IllegalArgumentException("La contraseña del empleado debe tener al menos 8 caracteres");
		}
		String email = comando.email() == null ? null : comando.email().trim().toLowerCase();
		if (email != null && usuarios.buscarPorEmail(email).isPresent()) {
			throw new EmailYaRegistrado(email);
		}
		return usuarios.guardar(
				Usuario.crear(comando.empresaId(), email, passwordEncoder.encode(comando.password()), comando.rol()));
	}
}
