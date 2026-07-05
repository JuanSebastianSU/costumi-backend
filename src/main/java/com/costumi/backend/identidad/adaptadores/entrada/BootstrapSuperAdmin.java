package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea al inicio un SuperAdmin si se configura email+password y aún no existe. Idempotente.
 * Vacío por defecto (no crea nada); en un despliegue se setea por entorno
 * (COSTUMI_SUPERADMIN_EMAIL / COSTUMI_SUPERADMIN_PASSWORD).
 */
@Component
class BootstrapSuperAdmin implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BootstrapSuperAdmin.class);

	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String password;

	BootstrapSuperAdmin(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
			@Value("${costumi.security.bootstrap.superadmin.email:}") String email,
			@Value("${costumi.security.bootstrap.superadmin.password:}") String password) {
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (email.isBlank() || password.isBlank()) {
			return; // no configurado: no se siembra
		}
		if (usuarios.buscarPorEmail(email).isPresent()) {
			return; // ya existe
		}
		usuarios.guardar(Usuario.crear(null, email, passwordEncoder.encode(password), Rol.SUPERADMIN));
		log.info("SuperAdmin de bootstrap creado: {}", email);
	}
}
