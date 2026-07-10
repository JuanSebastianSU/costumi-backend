package com.costumi.backend.identidad.dominio;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Pirámide de roles (RF-1.3, B3): quién puede gestionar/crear a quién. */
class RolTest {

	@Test
	void el_dueno_gestiona_encargados_y_operativos_pero_no_a_otro_dueno() {
		assertThat(Rol.DUENO.puedeGestionarA(Rol.ENCARGADO)).isTrue();
		assertThat(Rol.DUENO.puedeGestionarA(Rol.MOSTRADOR)).isTrue();
		assertThat(Rol.DUENO.puedeGestionarA(Rol.BODEGA)).isTrue();
		assertThat(Rol.DUENO.puedeGestionarA(Rol.ATENCION)).isTrue();
		assertThat(Rol.DUENO.puedeGestionarA(Rol.DUENO)).isFalse();
	}

	@Test
	void el_encargado_solo_gestiona_operativos_no_a_un_igual_ni_al_dueno() {
		assertThat(Rol.ENCARGADO.puedeGestionarA(Rol.MOSTRADOR)).isTrue();
		assertThat(Rol.ENCARGADO.puedeGestionarA(Rol.BODEGA)).isTrue();
		assertThat(Rol.ENCARGADO.puedeGestionarA(Rol.ATENCION)).isTrue();
		assertThat(Rol.ENCARGADO.puedeGestionarA(Rol.ENCARGADO)).isFalse(); // un igual
		assertThat(Rol.ENCARGADO.puedeGestionarA(Rol.DUENO)).isFalse();     // un superior
	}

	@Test
	void un_operativo_no_gestiona_a_nadie() {
		assertThat(Rol.MOSTRADOR.puedeGestionarA(Rol.MOSTRADOR)).isFalse();
		assertThat(Rol.MOSTRADOR.puedeGestionarA(Rol.BODEGA)).isFalse();
		assertThat(Rol.MOSTRADOR.puedeGestionarA(Rol.ENCARGADO)).isFalse();
	}

	@Test
	void nadie_crea_un_dueno_por_esta_via() {
		// Ningún rol está por encima del Dueño, así que puedeGestionarA(DUENO) es siempre falso.
		for (Rol rol : Rol.values()) {
			assertThat(rol.puedeGestionarA(Rol.DUENO)).isFalse();
		}
	}
}
