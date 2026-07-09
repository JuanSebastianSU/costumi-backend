package com.costumi.backend.inventario.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio de la Prenda: puras, sin BD ni Spring. */
class PrendaTest {

	private static final UUID EMPRESA = UUID.randomUUID();
	private static final UUID CATEGORIA = UUID.randomUUID();

	@Test
	void crear_una_prenda_de_renta() {
		Prenda prenda = Prenda.crear(EMPRESA, CATEGORIA, "Camisa pirata", TipoArticulo.RENTA,
				new BigDecimal("50.00"), null);

		assertThat(prenda.tipoArticulo()).isEqualTo(TipoArticulo.RENTA);
		assertThat(prenda.precioRenta()).isEqualByComparingTo("50.00");
		assertThat(prenda.precioVenta()).isNull();
	}

	@Test
	void una_prenda_de_renta_exige_precio_de_renta() {
		assertThatThrownBy(() -> Prenda.crear(EMPRESA, CATEGORIA, "X", TipoArticulo.RENTA, null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void una_prenda_de_ambos_exige_ambos_precios() {
		assertThatThrownBy(() -> Prenda.crear(EMPRESA, CATEGORIA, "X", TipoArticulo.AMBOS,
				new BigDecimal("10.00"), null))
				.isInstanceOf(IllegalArgumentException.class);

		Prenda ok = Prenda.crear(EMPRESA, CATEGORIA, "X", TipoArticulo.AMBOS,
				new BigDecimal("10.00"), new BigDecimal("100.00"));
		assertThat(ok.precioRenta()).isEqualByComparingTo("10.00");
		assertThat(ok.precioVenta()).isEqualByComparingTo("100.00");
	}

	@Test
	void el_precio_de_renta_debe_ser_mayor_a_cero() {
		assertThatThrownBy(() -> Prenda.crear(EMPRESA, CATEGORIA, "X", TipoArticulo.RENTA,
				BigDecimal.ZERO, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void el_nombre_es_obligatorio() {
		assertThatThrownBy(() -> Prenda.crear(EMPRESA, CATEGORIA, "  ", TipoArticulo.VENTA,
				null, new BigDecimal("10.00")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void una_prenda_lleva_sus_valores_de_etiqueta() {
		UUID color = UUID.randomUUID();
		UUID rojo = UUID.randomUUID();

		Prenda prenda = Prenda.crear(EMPRESA, CATEGORIA, "Camisa pirata", TipoArticulo.VENTA,
				null, new BigDecimal("10.00"), EtiquetasDePrenda.de(java.util.Map.of(color, rojo)));

		assertThat(prenda.etiquetas().valorDe(color)).contains(rojo);
	}

	@Test
	void una_prenda_sin_etiquetas_arranca_sin_clasificar() {
		Prenda prenda = Prenda.crear(EMPRESA, CATEGORIA, "Camisa", TipoArticulo.VENTA, null, new BigDecimal("10.00"));

		assertThat(prenda.etiquetas().esVacia()).isTrue();
	}

	@Test
	void lleva_costo_de_adquisicion_y_deposito_sugerido() {
		Prenda prenda = Prenda.crear(EMPRESA, CATEGORIA, "Traje", TipoArticulo.RENTA, new BigDecimal("50.00"), null,
				EtiquetasDePrenda.ninguna(), new BigDecimal("120.00"), new BigDecimal("200.00"), null, null);

		assertThat(prenda.costoAdquisicion()).isEqualByComparingTo("120.00");
		assertThat(prenda.depositoSugerido()).isEqualByComparingTo("200.00");
	}

	@Test
	void el_costo_de_adquisicion_no_puede_ser_negativo() {
		assertThatThrownBy(() -> Prenda.crear(EMPRESA, CATEGORIA, "Traje", TipoArticulo.RENTA, new BigDecimal("50.00"),
				null, EtiquetasDePrenda.ninguna(), new BigDecimal("-1.00"), null, null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void editar_actualiza_datos_y_revalida_contra_el_tipo() {
		Prenda prenda = Prenda.crear(EMPRESA, CATEGORIA, "Vieja", TipoArticulo.RENTA, new BigDecimal("50.00"), null);

		prenda.editar("Nueva", new BigDecimal("70.00"), null, new BigDecimal("120.00"), new BigDecimal("200.00"),
				new BigDecimal("300.00"), new BigDecimal("40.00"));

		assertThat(prenda.nombre()).isEqualTo("Nueva");
		assertThat(prenda.precioRenta()).isEqualByComparingTo("70.00");
		assertThat(prenda.valorReposicion()).isEqualByComparingTo("300.00");
		// Es de RENTA: editar sin precio de renta es inválido.
		assertThatThrownBy(() -> prenda.editar("X", null, null, null, null, null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void archivar_y_activar_cambia_el_estado() {
		Prenda prenda = Prenda.crear(EMPRESA, CATEGORIA, "P", TipoArticulo.RENTA, new BigDecimal("50.00"), null);
		assertThat(prenda.archivada()).isFalse();

		prenda.archivar();
		assertThat(prenda.archivada()).isTrue();

		prenda.activar();
		assertThat(prenda.archivada()).isFalse();
	}

	@Test
	void lleva_valores_de_multa_por_reposicion_y_dano() {
		Prenda prenda = Prenda.crear(EMPRESA, CATEGORIA, "Traje", TipoArticulo.RENTA, new BigDecimal("50.00"), null,
				EtiquetasDePrenda.ninguna(), null, null, new BigDecimal("300.00"), new BigDecimal("45.00"));

		assertThat(prenda.valorReposicion()).isEqualByComparingTo("300.00");
		assertThat(prenda.valorDano()).isEqualByComparingTo("45.00");
	}

	@Test
	void el_valor_de_reposicion_no_puede_ser_negativo() {
		assertThatThrownBy(() -> Prenda.crear(EMPRESA, CATEGORIA, "Traje", TipoArticulo.RENTA, new BigDecimal("50.00"),
				null, EtiquetasDePrenda.ninguna(), null, null, new BigDecimal("-1.00"), null))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
