package com.costumi.backend.notificaciones.dominio;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio de la Notificación: puras, sin BD ni Spring. */
class NotificacionTest {

	@Test
	void una_notificacion_nace_pendiente() {
		Notificacion n = Notificacion.crear(UUID.randomUUID(), UUID.randomUUID(), CanalNotificacion.WHATSAPP, "Hola");

		assertThat(n.estado()).isEqualTo(EstadoNotificacion.PENDIENTE);
	}

	@Test
	void marcar_enviada_cambia_el_estado() {
		Notificacion n = Notificacion.crear(UUID.randomUUID(), null, CanalNotificacion.FCM, "Aviso");

		n.marcarEnviada();

		assertThat(n.estado()).isEqualTo(EstadoNotificacion.ENVIADA);
	}

	@Test
	void el_mensaje_es_obligatorio() {
		assertThatThrownBy(() -> Notificacion.crear(UUID.randomUUID(), null, CanalNotificacion.EMAIL, "  "))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
