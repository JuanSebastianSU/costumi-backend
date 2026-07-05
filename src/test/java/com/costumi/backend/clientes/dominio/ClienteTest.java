package com.costumi.backend.clientes.dominio;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio del Cliente: puras, sin BD ni Spring. */
class ClienteTest {

	@Test
	void crear_un_cliente() {
		UUID empresa = UUID.randomUUID();

		Cliente cliente = Cliente.crear(empresa, "Juan Pérez", "3001234567", "juan@correo.com", "CC-123", "Calle 1");

		assertThat(cliente.empresaId()).isEqualTo(empresa);
		assertThat(cliente.nombre()).isEqualTo("Juan Pérez");
		assertThat(cliente.documento()).isEqualTo("CC-123");
		assertThat(cliente.enListaNegra()).isFalse();
	}

	@Test
	void los_campos_vacios_quedan_nulos() {
		Cliente cliente = Cliente.crear(UUID.randomUUID(), "Ana", "   ", "", null, "  ");

		assertThat(cliente.telefono()).isNull();
		assertThat(cliente.email()).isNull();
		assertThat(cliente.documento()).isNull();
		assertThat(cliente.direccion()).isNull();
	}

	@Test
	void poner_y_quitar_de_lista_negra() {
		Cliente cliente = Cliente.crear(UUID.randomUUID(), "Ana", null, null, null, null);

		cliente.ponerEnListaNegra();
		assertThat(cliente.enListaNegra()).isTrue();

		cliente.quitarDeListaNegra();
		assertThat(cliente.enListaNegra()).isFalse();
	}

	@Test
	void el_nombre_es_obligatorio() {
		assertThatThrownBy(() -> Cliente.crear(UUID.randomUUID(), "  ", null, null, null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
