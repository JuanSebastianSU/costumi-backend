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
	private final com.costumi.backend.identidad.ConsultaDeAsignaciones asignaciones;
	private final com.costumi.backend.clientes.ResolucionDeClientes clientes;
	private final org.springframework.context.ApplicationEventPublisher eventos;

	VentaService(VentaRepository ventas, ConsultaDeInventario inventario, AjusteDeInventario ajusteDeInventario,
			ConsultaDeConfiguracion configuracion, com.costumi.backend.identidad.ConsultaDeSucursales sucursales,
			com.costumi.backend.identidad.ConsultaDeAsignaciones asignaciones,
			com.costumi.backend.clientes.ResolucionDeClientes clientes,
			org.springframework.context.ApplicationEventPublisher eventos) {
		this.ventas = ventas;
		this.inventario = inventario;
		this.ajusteDeInventario = ajusteDeInventario;
		this.configuracion = configuracion;
		this.sucursales = sucursales;
		this.asignaciones = asignaciones;
		this.clientes = clientes;
		this.eventos = eventos;
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
		// B2 (RF-1.2): un empleado de rol acotado solo opera en las sucursales que tiene asignadas.
		if (!asignaciones.empleadoPuedeOperarEn(comando.empresaId(), comando.empleadoId(), comando.sucursalId())) {
			throw new com.costumi.backend.compartido.EmpleadoNoAsignadoASucursal(comando.sucursalId());
		}
		// SEC-2: si la venta va a nombre de un cliente, debe existir y ser de esta empresa (el cliente es
		// opcional: una venta de mostrador puede ser anónima, RF-4).
		if (comando.clienteId() != null && !clientes.existe(comando.empresaId(), comando.clienteId())) {
			throw new IllegalArgumentException("El cliente no existe en esta empresa");
		}
		// R-E: el personal no vende a nombre de una ficha archivada (retirada); reactivarla primero.
		if (comando.clienteId() != null && clientes.estaArchivado(comando.empresaId(), comando.clienteId())) {
			throw new com.costumi.backend.compartido.ClienteArchivado(comando.clienteId());
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
		Venta guardada = ventas.guardar(venta);
		// RF-11.1 (§5.5): si la venta es a nombre de un cliente, se avisa para el agradecimiento por compra.
		if (guardada.clienteId() != null) {
			eventos.publishEvent(new com.costumi.backend.ventas.VentaConfirmada(
					guardada.empresaId(), guardada.clienteId(), guardada.sucursalId()));
		}
		return guardada;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Venta> deEmpresa(UUID empresaId) {
		return ventas.listarPorEmpresa(empresaId);
	}

	@Override
	@Transactional(readOnly = true)
	public com.costumi.backend.compartido.Pagina<Venta> listar(UUID empresaId, String buscar,
			com.costumi.backend.compartido.SolicitudDePagina solicitud) {
		return ventas.listar(empresaId, buscar, solicitud);
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
	public Optional<Venta> buscarPorId(UUID empresaId, UUID ventaId) {
		return ventas.buscarPorId(ventaId).filter(venta -> venta.empresaId().equals(empresaId));
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
	public boolean estaDevuelta(UUID empresaId, UUID ventaId) {
		return ventas.buscarPorId(ventaId)
				.filter(venta -> venta.empresaId().equals(empresaId))
				.map(venta -> venta.estado() == com.costumi.backend.ventas.dominio.EstadoVenta.DEVUELTA
						|| venta.estado() == com.costumi.backend.ventas.dominio.EstadoVenta.PARCIALMENTE_DEVUELTA)
				.orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UUID> clienteDeVenta(UUID empresaId, UUID ventaId) {
		return ventas.buscarPorId(ventaId)
				.filter(venta -> venta.empresaId().equals(empresaId))
				.map(Venta::clienteId);
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
				.map(i -> LineaDeVenta.de(i.prendaId(), i.cantidad(), i.precioUnitario(),
						com.costumi.backend.ventas.dominio.OrigenDisfraz.rehidratar(
								i.disfrazId(), i.disfrazGrupo(), i.disfrazCantidad(), i.disfrazNombre())))
				.toList();
		Venta venta = ejecutar(new RegistrarVentaComando(empresaId, sucursalId, empleadoId, clienteId,
				BigDecimal.ZERO, lineas, null));
		return venta.id();
	}
}
