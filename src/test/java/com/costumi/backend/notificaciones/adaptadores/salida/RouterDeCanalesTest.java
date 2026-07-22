package com.costumi.backend.notificaciones.adaptadores.salida;

import com.costumi.backend.notificaciones.dominio.CanalNotificacion;
import com.costumi.backend.notificaciones.dominio.ContactoDelCliente;
import com.costumi.backend.notificaciones.dominio.Notificacion;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** El router está gateado: sin credenciales/contacto, cualquier canal cae al registro en log y confirma. */
class RouterDeCanalesTest {

	private final ContactoDelCliente sinContacto = (empresaId, clienteId) -> Optional.empty();

	private RouterDeCanales routerSinCredenciales() {
		return new RouterDeCanales(
				new CanalWhatsApp(sinContacto, "", ""),
				new CanalFcm(sinContacto, ""),
				new CanalDeNotificacionLog());
	}

	private Notificacion notificacion(CanalNotificacion canal) {
		return Notificacion.crear(UUID.randomUUID(), UUID.randomUUID(), canal, "Tu renta vence mañana");
	}

	@Test
	void whatsapp_sin_credenciales_cae_al_log() {
		assertThat(routerSinCredenciales().enviar(notificacion(CanalNotificacion.WHATSAPP))).isTrue();
	}

	@Test
	void fcm_sin_credenciales_cae_al_log() {
		assertThat(routerSinCredenciales().enviar(notificacion(CanalNotificacion.FCM))).isTrue();
	}

	@Test
	void email_va_al_log() {
		assertThat(routerSinCredenciales().enviar(notificacion(CanalNotificacion.EMAIL))).isTrue();
	}

	@Test
	void los_canales_no_estan_configurados_sin_credenciales() {
		assertThat(new CanalWhatsApp(sinContacto, "", "").configurado()).isFalse();
		assertThat(new CanalFcm(sinContacto, "").configurado()).isFalse();
	}
}
