package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.clientes.ResolucionDeClientes;
import com.costumi.backend.identidad.ConsultaDeJerarquiaDeRoles;
import com.costumi.backend.pagos.dominio.MetodoPago;
import com.costumi.backend.pagos.dominio.Pago;
import com.costumi.backend.pagos.dominio.PagoRepository;
import com.costumi.backend.pagos.dominio.PasarelaDePago;
import com.costumi.backend.pagos.dominio.SolicitudDeReembolso;
import com.costumi.backend.pagos.dominio.SolicitudDeReembolsoRepository;
import com.costumi.backend.pagos.dominio.TipoConcepto;
import com.costumi.backend.pagos.dominio.TipoPago;
import com.costumi.backend.rentas.ConsultaDeRentas;
import com.costumi.backend.ventas.ConsultaDeVentas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Proceso de reembolso en dos pasos (RF-4.5/6.9): validaciones, precondición, escalamiento y ejecución. */
class ReembolsoServiceTest {

	private SolicitudDeReembolsoRepository solicitudes;
	private PagoRepository pagos;
	private RegistrarPago registrarPago;
	private PasarelaDePago pasarela;
	private ConsultaDeVentas ventas;
	private ConsultaDeRentas rentas;
	private ConsultaDeJerarquiaDeRoles jerarquia;
	private ResolucionDeClientes clientes;
	private ReembolsoService service;

	private final UUID empresa = UUID.randomUUID();
	private final UUID venta = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		solicitudes = mock(SolicitudDeReembolsoRepository.class);
		pagos = mock(PagoRepository.class);
		registrarPago = mock(RegistrarPago.class);
		pasarela = mock(PasarelaDePago.class);
		ventas = mock(ConsultaDeVentas.class);
		rentas = mock(ConsultaDeRentas.class);
		jerarquia = mock(ConsultaDeJerarquiaDeRoles.class);
		clientes = mock(ResolucionDeClientes.class);
		service = new ReembolsoService(solicitudes, pagos, registrarPago, pasarela, ventas, rentas, jerarquia,
				clientes);
		when(solicitudes.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
	}

	private SolicitarReembolsoComando solicitud(BigDecimal monto) {
		return new SolicitarReembolsoComando(empresa, TipoConcepto.VENTA, venta, null, monto, "no le quedó");
	}

	@Test
	void solicitar_con_saldo_suficiente_crea_pendiente() {
		when(pagos.saldoNetoPorConcepto(empresa, venta)).thenReturn(new BigDecimal("100.00"));
		when(solicitudes.existePendientePorConcepto(empresa, venta)).thenReturn(false);

		SolicitudDeReembolso s = service.ejecutar(solicitud(new BigDecimal("80.00")));

		assertThat(s.estaPendiente()).isTrue();
		assertThat(s.monto()).isEqualByComparingTo("80.00");
	}

	@Test
	void solicitar_por_mas_del_saldo_falla() {
		when(pagos.saldoNetoPorConcepto(empresa, venta)).thenReturn(new BigDecimal("50.00"));

		assertThatThrownBy(() -> service.ejecutar(solicitud(new BigDecimal("80.00"))))
				.isInstanceOf(SolicitudDeReembolsoInvalida.class);
	}

	@Test
	void solicitar_con_otra_pendiente_falla() {
		when(pagos.saldoNetoPorConcepto(empresa, venta)).thenReturn(new BigDecimal("100.00"));
		when(solicitudes.existePendientePorConcepto(empresa, venta)).thenReturn(true);

		assertThatThrownBy(() -> service.ejecutar(solicitud(new BigDecimal("80.00"))))
				.isInstanceOf(SolicitudDeReembolsoInvalida.class);
	}

	@Test
	void aprobar_sin_item_devuelto_falla_y_no_reembolsa() {
		SolicitudDeReembolso s = pendienteGuardada(new BigDecimal("80.00"));
		when(ventas.estaDevuelta(empresa, venta)).thenReturn(false);

		assertThatThrownBy(() -> service.ejecutar(decidir(s.id(), true, "ok", UUID.randomUUID(), "ENCARGADO")))
				.isInstanceOf(ItemNoDevuelto.class);
		verify(registrarPago, never()).ejecutar(any());
	}

	@Test
	void aprobar_con_item_devuelto_registra_el_reembolso() {
		SolicitudDeReembolso s = pendienteGuardada(new BigDecimal("80.00"));
		when(ventas.estaDevuelta(empresa, venta)).thenReturn(true);
		when(pagos.listarPorConcepto(empresa, venta)).thenReturn(List.of(cobroEfectivo()));

		SolicitudDeReembolso decidida = service.ejecutar(
				decidir(s.id(), true, "aprobado", UUID.randomUUID(), "ENCARGADO"));

		assertThat(decidida.estado().name()).isEqualTo("APROBADA");
		verify(registrarPago).ejecutar(any());
		verify(pasarela, never()).reembolsar(any(), any()); // pago en efectivo: sin refund a tarjeta
	}

	@Test
	void aprobar_un_cobro_con_tarjeta_y_pasarela_configurada_reembolsa_al_plastico() {
		SolicitudDeReembolso s = pendienteGuardada(new BigDecimal("80.00"));
		when(ventas.estaDevuelta(empresa, venta)).thenReturn(true);
		when(pasarela.configurada()).thenReturn(true);
		when(pagos.listarPorConcepto(empresa, venta)).thenReturn(List.of(cobroTarjeta("MP-999")));

		service.ejecutar(decidir(s.id(), true, "aprobado", UUID.randomUUID(), "ENCARGADO"));

		verify(pasarela).reembolsar(eq("MP-999"), any());
	}

	@Test
	void rechazar_deja_la_solicitud_rechazada() {
		SolicitudDeReembolso s = pendienteGuardada(new BigDecimal("80.00"));

		SolicitudDeReembolso decidida = service.ejecutar(
				decidir(s.id(), false, "fuera de plazo", UUID.randomUUID(), "MOSTRADOR"));

		assertThat(decidida.estaRechazada()).isTrue();
	}

	@Test
	void un_rol_que_no_supera_no_puede_revertir_un_rechazo() {
		SolicitudDeReembolso s = pendienteGuardada(new BigDecimal("80.00"));
		service.ejecutar(decidir(s.id(), false, "no", UUID.randomUUID(), "MOSTRADOR"));
		when(jerarquia.superaEstrictamente("MOSTRADOR", "MOSTRADOR")).thenReturn(false);

		assertThatThrownBy(() -> service.ejecutar(decidir(s.id(), true, "insisto", UUID.randomUUID(), "MOSTRADOR")))
				.isInstanceOf(DecisionDeReembolsoNoPermitida.class);
	}

	@Test
	void un_rol_superior_puede_revertir_un_rechazo() {
		SolicitudDeReembolso s = pendienteGuardada(new BigDecimal("80.00"));
		service.ejecutar(decidir(s.id(), false, "no", UUID.randomUUID(), "MOSTRADOR"));
		when(jerarquia.superaEstrictamente("ENCARGADO", "MOSTRADOR")).thenReturn(true);
		when(ventas.estaDevuelta(empresa, venta)).thenReturn(true);
		when(pagos.listarPorConcepto(empresa, venta)).thenReturn(List.of(cobroEfectivo()));

		SolicitudDeReembolso decidida = service.ejecutar(
				decidir(s.id(), true, "lo apruebo", UUID.randomUUID(), "ENCARGADO"));

		assertThat(decidida.estado().name()).isEqualTo("APROBADA");
	}

	@Test
	void un_cliente_solicita_el_reembolso_de_su_propia_venta() {
		UUID usuario = UUID.randomUUID();
		UUID ficha = UUID.randomUUID();
		when(clientes.fichaDeUsuarioSiExiste(empresa, usuario)).thenReturn(Optional.of(ficha));
		when(ventas.clienteDeVenta(empresa, venta)).thenReturn(Optional.of(ficha)); // la venta es suya
		when(pagos.saldoNetoPorConcepto(empresa, venta)).thenReturn(new BigDecimal("100.00"));
		when(solicitudes.existePendientePorConcepto(empresa, venta)).thenReturn(false);

		SolicitudDeReembolso s = service.ejecutar(new SolicitarReembolsoDeClienteComando(empresa, usuario,
				"c@x.com", TipoConcepto.VENTA, venta, new BigDecimal("50.00"), "no me quedó"));

		assertThat(s.estaPendiente()).isTrue();
		assertThat(s.solicitanteClienteId()).isEqualTo(ficha);
	}

	@Test
	void un_cliente_no_puede_solicitar_el_reembolso_de_una_venta_ajena() {
		UUID usuario = UUID.randomUUID();
		when(clientes.fichaDeUsuarioSiExiste(empresa, usuario)).thenReturn(Optional.of(UUID.randomUUID()));
		when(ventas.clienteDeVenta(empresa, venta)).thenReturn(Optional.of(UUID.randomUUID())); // de otro cliente

		assertThatThrownBy(() -> service.ejecutar(new SolicitarReembolsoDeClienteComando(empresa, usuario,
				"c@x.com", TipoConcepto.VENTA, venta, new BigDecimal("50.00"), "dame la plata")))
				.isInstanceOf(ReembolsoNoAutorizado.class);
	}

	@Test
	void un_cliente_sin_ficha_en_la_empresa_no_puede_solicitar() {
		UUID usuario = UUID.randomUUID();
		when(clientes.fichaDeUsuarioSiExiste(empresa, usuario)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.ejecutar(new SolicitarReembolsoDeClienteComando(empresa, usuario,
				"c@x.com", TipoConcepto.VENTA, venta, new BigDecimal("50.00"), "hola")))
				.isInstanceOf(ReembolsoNoAutorizado.class);
	}

	// --- helpers ---

	private SolicitudDeReembolso pendienteGuardada(BigDecimal monto) {
		SolicitudDeReembolso s = SolicitudDeReembolso.crear(empresa, TipoConcepto.VENTA, venta, null, monto, "motivo");
		when(solicitudes.buscarPorId(s.id())).thenReturn(Optional.of(s));
		return s;
	}

	private DecidirReembolsoComando decidir(UUID id, boolean aprobar, String motivo, UUID actor, String rol) {
		return new DecidirReembolsoComando(empresa, id, aprobar, actor, rol, motivo);
	}

	private Pago cobroEfectivo() {
		return Pago.registrar(empresa, UUID.randomUUID(), UUID.randomUUID(), TipoConcepto.VENTA, venta,
				new BigDecimal("100.00"), TipoPago.COBRO, MetodoPago.EFECTIVO, null, "cobro-1");
	}

	private Pago cobroTarjeta(String idExterno) {
		return Pago.registrar(empresa, UUID.randomUUID(), UUID.randomUUID(), TipoConcepto.VENTA, venta,
				new BigDecimal("100.00"), TipoPago.COBRO, MetodoPago.TARJETA, "pasarela:" + idExterno,
				"pasarela:" + idExterno);
	}
}
