package com.costumi.backend.notificaciones.dominio;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio de la plantilla: render de variables, edición y default. Puras, sin BD ni Spring. */
class PlantillaDeNotificacionTest {

	private static final UUID EMPRESA = UUID.randomUUID();

	@Test
	void render_reemplaza_las_variables_provistas() {
		PlantillaDeNotificacion p = PlantillaDeNotificacion.rehidratar(UUID.randomUUID(), EMPRESA,
				TipoDeEvento.MULTA_GENERADA, "Hola {cliente}, debes {monto}.", true);

		String texto = p.render(Map.of("cliente", "Ana Torres", "monto", "$50"));

		assertThat(texto).isEqualTo("Hola Ana Torres, debes $50.");
	}

	@Test
	void render_deja_las_variables_no_provistas_como_estan() {
		PlantillaDeNotificacion p = PlantillaDeNotificacion.rehidratar(UUID.randomUUID(), EMPRESA,
				TipoDeEvento.RENTA_CONFIRMADA, "Devolvela el {fecha_devolucion} en {direccion}.", true);

		String texto = p.render(Map.of("fecha_devolucion", "2026-09-01"));

		assertThat(texto).isEqualTo("Devolvela el 2026-09-01 en {direccion}.");
	}

	@Test
	void por_defecto_usa_el_texto_del_tipo_y_nace_activa() {
		PlantillaDeNotificacion p = PlantillaDeNotificacion.porDefecto(EMPRESA, TipoDeEvento.RENTA_VENCIDA);

		assertThat(p.tipo()).isEqualTo(TipoDeEvento.RENTA_VENCIDA);
		assertThat(p.texto()).isEqualTo(TipoDeEvento.RENTA_VENCIDA.textoPorDefecto());
		assertThat(p.activa()).isTrue();
	}

	@Test
	void editar_cambia_texto_y_switch() {
		PlantillaDeNotificacion p = PlantillaDeNotificacion.porDefecto(EMPRESA, TipoDeEvento.COMPRA_REALIZADA);

		p.editar("Gracias {cliente}", false);

		assertThat(p.texto()).isEqualTo("Gracias {cliente}");
		assertThat(p.activa()).isFalse();
	}

	@Test
	void texto_vacio_es_invalido() {
		assertThatThrownBy(() -> PlantillaDeNotificacion.porDefecto(EMPRESA, TipoDeEvento.MULTA_GENERADA)
				.editar("  ", true))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
