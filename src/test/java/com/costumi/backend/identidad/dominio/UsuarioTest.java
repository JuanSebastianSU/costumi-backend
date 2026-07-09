package com.costumi.backend.identidad.dominio;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio del Usuario: puras, sin BD ni Spring. */
class UsuarioTest {

	@Test
	void crear_un_usuario_de_empresa() {
		UUID empresaId = UUID.randomUUID();

		Usuario usuario = Usuario.crear(empresaId, "duenio@empresa.com", "hash", Rol.DUENO);

		assertThat(usuario.id()).isNotNull();
		assertThat(usuario.empresaId()).isEqualTo(empresaId);
		assertThat(usuario.email()).isEqualTo("duenio@empresa.com");
		assertThat(usuario.rol()).isEqualTo(Rol.DUENO);
	}

	@Test
	void el_superadmin_no_pertenece_a_una_empresa() {
		Usuario superAdmin = Usuario.crear(null, "sa@plataforma.com", "hash", Rol.SUPERADMIN);

		assertThat(superAdmin.empresaId()).isNull();
	}

	@Test
	void un_superadmin_con_empresa_es_invalido() {
		assertThatThrownBy(() -> Usuario.crear(UUID.randomUUID(), "sa@x.com", "hash", Rol.SUPERADMIN))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void un_usuario_de_empresa_sin_empresa_es_invalido() {
		assertThatThrownBy(() -> Usuario.crear(null, "u@x.com", "hash", Rol.MOSTRADOR))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void el_email_es_obligatorio() {
		assertThatThrownBy(() -> Usuario.crear(UUID.randomUUID(), "   ", "hash", Rol.DUENO))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void un_usuario_nace_activo_y_se_puede_desactivar_y_reactivar() {
		Usuario usuario = Usuario.crear(UUID.randomUUID(), "emp@empresa.com", "hash", Rol.MOSTRADOR);
		assertThat(usuario.activo()).isTrue();

		Usuario baja = usuario.desactivar();
		assertThat(baja.activo()).isFalse();
		// Misma cuenta (id/email/rol), solo cambia el estado.
		assertThat(baja.id()).isEqualTo(usuario.id());
		assertThat(baja.email()).isEqualTo(usuario.email());
		assertThat(baja.rol()).isEqualTo(usuario.rol());

		assertThat(baja.activar().activo()).isTrue();
	}

	@Test
	void cambiar_contrasena_conserva_el_estado_activo() {
		Usuario baja = Usuario.crear(null, "cli@x.com", "hash", Rol.CLIENTE).desactivar();
		assertThat(baja.cambiarContrasena("otro").activo()).isFalse();
	}
}
