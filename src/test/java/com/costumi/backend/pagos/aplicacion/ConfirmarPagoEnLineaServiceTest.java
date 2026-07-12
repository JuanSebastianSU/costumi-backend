package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.IntentoDePago;
import com.costumi.backend.pagos.dominio.IntentoDePagoRepository;
import com.costumi.backend.pagos.dominio.Pago;
import com.costumi.backend.pagos.dominio.PasarelaDePago;
import com.costumi.backend.pagos.dominio.TipoConcepto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** P-3: la confirmación desde el webhook verifica contra el proveedor cuando la pasarela está configurada. */
class ConfirmarPagoEnLineaServiceTest {

	private final BigDecimal monto = new BigDecimal("100.00");

	private IntentoDePago intentoPendiente() {
		return IntentoDePago.crear(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				TipoConcepto.RENTA, UUID.randomUUID(), monto, "ARS");
	}

	@Test
	void sin_pasarela_configurada_confia_en_el_webhook_y_confirma() {
		IntentoDePago intento = intentoPendiente();
		IntentosFake intentos = new IntentosFake(intento);
		RegistrarPagoFake registrar = new RegistrarPagoFake();
		var service = new ConfirmarPagoEnLineaService(intentos, registrar, new PasarelaFake(false, null));

		service.ejecutar(intento.id(), "mp-123");

		assertThat(registrar.llamado).isTrue();
		assertThat(intento.estaConfirmado()).isTrue();
	}

	@Test
	void con_pasarela_configurada_y_pago_aprobado_del_monto_correcto_confirma() {
		IntentoDePago intento = intentoPendiente();
		IntentosFake intentos = new IntentosFake(intento);
		RegistrarPagoFake registrar = new RegistrarPagoFake();
		var service = new ConfirmarPagoEnLineaService(intentos, registrar,
				new PasarelaFake(true, new PasarelaDePago.EstadoPagoExterno(true, monto)));

		service.ejecutar(intento.id(), "mp-123");

		assertThat(registrar.llamado).isTrue();
		assertThat(intento.estaConfirmado()).isTrue();
	}

	@Test
	void con_pasarela_configurada_y_pago_no_aprobado_no_confirma() {
		IntentoDePago intento = intentoPendiente();
		IntentosFake intentos = new IntentosFake(intento);
		RegistrarPagoFake registrar = new RegistrarPagoFake();
		var service = new ConfirmarPagoEnLineaService(intentos, registrar,
				new PasarelaFake(true, new PasarelaDePago.EstadoPagoExterno(false, null)));

		service.ejecutar(intento.id(), "mp-123");

		assertThat(registrar.llamado).isFalse();
		assertThat(intento.estaConfirmado()).isFalse();
	}

	@Test
	void con_pasarela_configurada_y_monto_distinto_falla_y_no_confirma() {
		IntentoDePago intento = intentoPendiente();
		IntentosFake intentos = new IntentosFake(intento);
		RegistrarPagoFake registrar = new RegistrarPagoFake();
		var service = new ConfirmarPagoEnLineaService(intentos, registrar,
				new PasarelaFake(true, new PasarelaDePago.EstadoPagoExterno(true, new BigDecimal("999.00"))));

		assertThatThrownBy(() -> service.ejecutar(intento.id(), "mp-123"))
				.isInstanceOf(VerificacionDePagoFallida.class);
		assertThat(registrar.llamado).isFalse();
		assertThat(intento.estaConfirmado()).isFalse();
	}

	@Test
	void un_intento_ya_confirmado_es_idempotente() {
		IntentoDePago intento = intentoPendiente();
		intento.confirmar();
		RegistrarPagoFake registrar = new RegistrarPagoFake();
		// Aunque la pasarela rechazaría, ni siquiera se consulta: ya está confirmado.
		var service = new ConfirmarPagoEnLineaService(new IntentosFake(intento), registrar,
				new PasarelaFake(true, new PasarelaDePago.EstadoPagoExterno(false, null)));

		service.ejecutar(intento.id(), "mp-123");

		assertThat(registrar.llamado).isFalse();
	}

	// --- Dobles ---

	private static final class IntentosFake implements IntentoDePagoRepository {
		private final IntentoDePago intento;

		IntentosFake(IntentoDePago intento) {
			this.intento = intento;
		}

		@Override
		public IntentoDePago guardar(IntentoDePago i) {
			return i;
		}

		@Override
		public Optional<IntentoDePago> buscarPorId(UUID id) {
			return id.equals(intento.id()) ? Optional.of(intento) : Optional.empty();
		}
	}

	private static final class RegistrarPagoFake implements RegistrarPago {
		private boolean llamado;

		@Override
		public Pago ejecutar(RegistrarPagoComando comando) {
			this.llamado = true;
			return null;
		}
	}

	private record PasarelaFake(boolean configurada, EstadoPagoExterno estado) implements PasarelaDePago {
		@Override
		public boolean configurada() {
			return configurada;
		}

		@Override
		public ResultadoCheckout crearCheckout(BigDecimal monto, String moneda, String referencia, String descripcion) {
			throw new UnsupportedOperationException();
		}

		@Override
		public EstadoPagoExterno consultarPago(String idPagoExterno) {
			return estado;
		}

		@Override
		public void reembolsar(String idPagoExterno, BigDecimal monto) {
			throw new UnsupportedOperationException();
		}
	}
}
