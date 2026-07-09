package com.costumi.backend.catalogo.dominio;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio del TipoEtiqueta y ValorEtiqueta: puras, sin BD ni Spring. */
class TipoEtiquetaTest {

	@Test
	void crear_un_tipo_con_sus_interruptores() {
		UUID empresaId = UUID.randomUUID();

		TipoEtiqueta color = TipoEtiqueta.crear(empresaId, "Color", true, true);

		assertThat(color.empresaId()).isEqualTo(empresaId);
		assertThat(color.nombre()).isEqualTo("Color");
		assertThat(color.defineVariante()).isTrue();
		assertThat(color.seleccionablePorCliente()).isTrue();
		assertThat(color.archivada()).isFalse();
	}

	@Test
	void el_nombre_del_tipo_es_obligatorio() {
		assertThatThrownBy(() -> TipoEtiqueta.crear(UUID.randomUUID(), " ", false, false))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void un_tipo_sin_categorias_aplica_a_todas() {
		TipoEtiqueta color = TipoEtiqueta.crear(UUID.randomUUID(), "Color", true, true);

		assertThat(color.categoriasQueAplica()).isEmpty();
		assertThat(color.aplicaACategoria(UUID.randomUUID())).isTrue();
	}

	@Test
	void un_tipo_con_categorias_solo_aplica_a_esas() {
		UUID camisas = UUID.randomUUID();
		TipoEtiqueta cuello = TipoEtiqueta.crear(UUID.randomUUID(), "Cuello", false, false, java.util.Set.of(camisas));

		assertThat(cuello.aplicaACategoria(camisas)).isTrue();
		assertThat(cuello.aplicaACategoria(UUID.randomUUID())).isFalse();
	}

	@Test
	void crear_un_valor_ligado_a_su_tipo() {
		UUID empresaId = UUID.randomUUID();
		UUID tipoId = UUID.randomUUID();

		ValorEtiqueta rojo = ValorEtiqueta.crear(empresaId, tipoId, "Rojo");

		assertThat(rojo.empresaId()).isEqualTo(empresaId);
		assertThat(rojo.tipoEtiquetaId()).isEqualTo(tipoId);
		assertThat(rojo.valor()).isEqualTo("Rojo");
	}

	@Test
	void el_valor_es_obligatorio() {
		assertThatThrownBy(() -> ValorEtiqueta.crear(UUID.randomUUID(), UUID.randomUUID(), "  "))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void renombrar_un_valor_conserva_su_identidad() {
		ValorEtiqueta rojo = ValorEtiqueta.crear(UUID.randomUUID(), UUID.randomUUID(), "Rojo");
		UUID id = rojo.id();

		rojo.renombrar("Escarlata");

		assertThat(rojo.valor()).isEqualTo("Escarlata");
		assertThat(rojo.id()).isEqualTo(id);
	}

	@Test
	void archivar_y_activar_un_tipo_y_un_valor() {
		TipoEtiqueta tipo = TipoEtiqueta.crear(UUID.randomUUID(), "Color", true, true);
		tipo.archivar();
		assertThat(tipo.archivada()).isTrue();
		tipo.activar();
		assertThat(tipo.archivada()).isFalse();

		ValorEtiqueta valor = ValorEtiqueta.crear(UUID.randomUUID(), UUID.randomUUID(), "Rojo");
		valor.archivar();
		assertThat(valor.archivada()).isTrue();
		valor.activar();
		assertThat(valor.archivada()).isFalse();
	}
}
