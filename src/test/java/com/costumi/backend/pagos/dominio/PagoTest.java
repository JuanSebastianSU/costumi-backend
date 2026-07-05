package com.costumi.backend.pagos.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de dominio del Pago: puras, sin BD ni Spring. */
class PagoTest {

	private static Pago pago(BigDecimal monto) {
		return Pago.registrar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), TipoConcepto.RENTA,
				UUID.randomUUID(), monto, MetodoPago.EFECTIVO, null, null);
	}

	@Test
	void registrar_un_pago_valido() {
		Pago p = pago(new BigDecimal("40.00"));

		assertThat(p.monto()).isEqualByComparingTo("40.00");
		assertThat(p.metodo()).isEqualTo(MetodoPago.EFECTIVO);
		assertThat(p.fecha()).isNotNull();
	}

	@Test
	void el_monto_debe_ser_positivo() {
		assertThatThrownBy(() -> pago(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> pago(new BigDecimal("-1.00"))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void la_referencia_vacia_queda_nula() {
		Pago p = Pago.registrar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), TipoConcepto.VENTA,
				UUID.randomUUID(), new BigDecimal("10.00"), MetodoPago.TARJETA, "   ", null);

		assertThat(p.referencia()).isNull();
	}
}
