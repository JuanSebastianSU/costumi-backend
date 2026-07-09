package com.costumi.backend.catalogo.dominio;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio de la Categoría: puras, sin BD ni Spring. */
class CategoriaTest {

	@Test
	void crear_una_categoria() {
		UUID empresaId = UUID.randomUUID();

		Categoria categoria = Categoria.crear(empresaId, "Camisa");

		assertThat(categoria.id()).isNotNull();
		assertThat(categoria.empresaId()).isEqualTo(empresaId);
		assertThat(categoria.nombre()).isEqualTo("Camisa");
		assertThat(categoria.archivada()).isFalse();
	}

	@Test
	void archivar_no_borra_y_activar_la_restituye() {
		Categoria categoria = Categoria.crear(UUID.randomUUID(), "Sombrero");

		categoria.archivar();
		assertThat(categoria.archivada()).isTrue();

		categoria.activar();
		assertThat(categoria.archivada()).isFalse();
	}

	@Test
	void renombrar_conserva_la_identidad() {
		Categoria categoria = Categoria.crear(UUID.randomUUID(), "Panton");
		UUID id = categoria.id();

		categoria.renombrar("Pantalón");

		assertThat(categoria.id()).isEqualTo(id);
		assertThat(categoria.nombre()).isEqualTo("Pantalón");
	}

	@Test
	void el_nombre_es_obligatorio() {
		assertThatThrownBy(() -> Categoria.crear(UUID.randomUUID(), "  "))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
