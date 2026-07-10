package com.costumi.backend.ventas.aplicacion;

import com.costumi.backend.configuracion.ConsultaDeConfiguracion;
import com.costumi.backend.inventario.AjusteDeInventario;
import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.ventas.ConsultaDeVentas;
import com.costumi.backend.ventas.RegistroDeVentas;
import com.costumi.backend.ventas.dominio.LineaDeVenta;
import com.costumi.backend.ventas.dominio.ReembolsoNoPermitido;
import com.costumi.backend.ventas.dominio.Venta;
import com.costumi.backend.ventas.dominio.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Casos de uso de Ventas, acotados a la empresa (tenant). */
@Service
class VentaService implements RegistrarVenta, ConsultarVentas, RegistroDeVentas, ConsultaDeVentas, DevolverVenta {

	private final VentaRepository ventas;
	private final ConsultaDeInventario inventario;
	private final AjusteDeInventario ajusteDeInventario;
	private final ConsultaDeConfiguracion configuracion;
	private final com.costumi.backend.identidad.ConsultaDeSucursales sucursales;

	VentaService(VentaRepository ventas, ConsultaDeInventario inventario, AjusteDeInventario ajusteDeInventario,
			ConsultaDeConfiguracion configuracion, com.costumi.backend.identidad.ConsultaDeSucursales sucursales) {
		this.ventas = ventas;
		this.inventario = inventario;
		this.ajusteDeInventario = ajusteDeInventario;
		this.configuracion = configuracion;
		this.sucursales = sucursales;
	}

	@Override
	@Transactional
	public Venta ejecutar(RegistrarVentaComando comando) {
		// Idempotencia (RF-17.6, offline/outbox): si la venta ya se registró con esta clave, no se duplica.
		if (comando.claveIdempotencia() != null && !comando.claveIdempotencia().isBlank()) {
			Optional<Venta> existente = ventas.buscarPorClave(comando.empresaId(), comando.claveIdempotencia().trim());
			if (existente.isPresent()) {
				return existente.get();
			}
		}
		// SEC-1: la venta debe anclarse a una sucursal existente, del tenant y activa.
		if (!sucursales.existeActiva(comando.empresaId(), comando.sucursalId())) {
			throw new IllegalArgumentException("La sucursal no existe o está archivada en esta empresa");
		}
		for (LineaDeVenta linea : comando.lineas()) {
			if (!inventario.prendaExiste(comando.empresaId(), linea.prendaId())) {
				throw new IllegalArgumentException("La prenda no existe en esta empresa");
			}
		}
		String clave = (comando.claveIdempotencia() != null && !comando.claveIdempotencia().isBlank())
				? comando.claveIdempotencia().trim() : null;
		Venta venta = Venta.crear(comando.empresaId(), comando.sucursalId(), comando.empleadoId(),
				comando.clienteId(), comando.descuento(), comando.lineas(), clave);
		// RF-12.4: solo se descuenta stock si la empresa cuenta stock; si no, la venta no toca inventario.
		if (configuracion.conteoStock(comando.empresaId())) {
			// Baja de stock al confirmar (RF-4.4): si no alcanza, StockInsuficiente revierte toda la venta.
			for (LineaDeVenta linea : comando.lineas()) {
				ajusteDeInventario.descontarDisponibles(comando.empresaId(), comando.sucursalId(), linea.prendaId(), linea.cantidad());
			}
		}
		return ventas.guardar(venta);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Venta> deEmpresa(UUID empresaId) {
		return ventas.listarPorEmpresa(empresaId);
	}

	@Override
	@Transactional
	public Venta devolver(UUID empresaId, UUID ventaId, Map<UUID, Integer> cantidades) {
		Venta venta = ventas.buscarPorId(ventaId)
				.filter(v -> v.empresaId().equals(empresaId))
				.orElseThrow(() -> new VentaNoEncontrada(ventaId));
		// Política de reembolso del local (RF-4.5): reembolsos activos y, si hay ventana, dentro del plazo.
		long diasDesdeLaVenta = ChronoUnit.DAYS.between(venta.creadaEn().atZone(ZoneOffset.UTC).toLocalDate(),
				java.time.LocalDate.now(ZoneOffset.UTC));
		if (!configuracion.reembolsoPermitido(empresaId, diasDesdeLaVenta)) {
			throw new ReembolsoNoPermitido(ventaId);
		}
		// Devuelve las unidades indicadas (o todo lo pendiente) y actualiza el estado (parcial/total, RF-4.5).
		Map<UUID, Integer> devueltas = venta.devolver(cantidades);
		// Reingresa al stock SOLO lo devuelto (si la empresa cuenta stock, igual que al confirmar la venta).
		if (configuracion.conteoStock(empresaId)) {
			devueltas.forEach((prendaId, cantidad) ->
					ajusteDeInventario.reingresarDisponibles(empresaId, venta.sucursalId(), prendaId, cantidad));
		}
		return ventas.guardar(venta);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<BigDecimal> totalDeVenta(UUID empresaId, UUID ventaId) {
		return ventas.buscarPorId(ventaId)
				.filter(venta -> venta.empresaId().equals(empresaId))
				.map(Venta::total);
	}

	@Override
	@Transactional(readOnly = true)
	public ActividadDeEmpleado actividadDeEmpleado(UUID empresaId, UUID empleadoId) {
		List<Venta> deEmpleado = ventas.listarPorEmpresa(empresaId).stream()
				.filter(venta -> empleadoId.equals(venta.empleadoId())
						&& venta.estado() == com.costumi.backend.ventas.dominio.EstadoVenta.CONFIRMADA)
				.toList();
		BigDecimal total = deEmpleado.stream().map(Venta::total).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new ActividadDeEmpleado(deEmpleado.size(), total);
	}

	@Override
	@Transactional
	public UUID registrar(UUID empresaId, UUID sucursalId, UUID empleadoId, UUID clienteId, List<ItemDeVenta> items) {
		List<LineaDeVenta> lineas = items.stream()
				.map(i -> LineaDeVenta.de(i.prendaId(), i.cantidad(), i.precioUnitario()))
				.toList();
		Venta venta = ejecutar(new RegistrarVentaComando(empresaId, sucursalId, empleadoId, clienteId,
				BigDecimal.ZERO, lineas, null));
		return venta.id();
	}
}
