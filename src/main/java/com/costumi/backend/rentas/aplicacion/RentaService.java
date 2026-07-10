package com.costumi.backend.rentas.aplicacion;

import com.costumi.backend.configuracion.ConsultaDeConfiguracion;
import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.rentas.ConsultaDeRentas;
import com.costumi.backend.rentas.RegistroDeRentas;
import com.costumi.backend.rentas.dominio.EstadoRenta;
import com.costumi.backend.rentas.dominio.Renta;
import com.costumi.backend.rentas.dominio.RentaLinea;
import com.costumi.backend.rentas.dominio.RentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/** Casos de uso de Rentas: crear, listar y transiciones de estado, acotados a la empresa (tenant). */
@Service
class RentaService implements CrearRenta, ConsultarRentas, GestionarRenta, ConsultaDeRentas, RegistroDeRentas {

	private final RentaRepository rentas;
	private final ConsultaDeInventario inventario;
	private final ConsultaDeConfiguracion configuracion;
	private final com.costumi.backend.identidad.ConsultaDeSucursales sucursales;
	private final com.costumi.backend.identidad.ConsultaDeAsignaciones asignaciones;
	private final com.costumi.backend.clientes.ResolucionDeClientes clientes;

	RentaService(RentaRepository rentas, ConsultaDeInventario inventario, ConsultaDeConfiguracion configuracion,
			com.costumi.backend.identidad.ConsultaDeSucursales sucursales,
			com.costumi.backend.identidad.ConsultaDeAsignaciones asignaciones,
			com.costumi.backend.clientes.ResolucionDeClientes clientes) {
		this.rentas = rentas;
		this.inventario = inventario;
		this.configuracion = configuracion;
		this.sucursales = sucursales;
		this.asignaciones = asignaciones;
		this.clientes = clientes;
	}

	@Override
	@Transactional
	public Renta ejecutar(CrearRentaComando comando) {
		if (comando.claveIdempotencia() != null && !comando.claveIdempotencia().isBlank()) {
			var existente = rentas.buscarPorClave(comando.empresaId(), comando.claveIdempotencia().trim());
			if (existente.isPresent()) {
				return existente.get(); // idempotente: no se duplica la renta (RF-17.6)
			}
		}
		if (comando.lineas() == null || comando.lineas().isEmpty()) {
			throw new IllegalArgumentException("La renta debe tener al menos un artículo");
		}
		// SEC-1: la renta debe anclarse a una sucursal existente, del tenant y activa.
		if (!sucursales.existeActiva(comando.empresaId(), comando.sucursalId())) {
			throw new IllegalArgumentException("La sucursal no existe o está archivada en esta empresa");
		}
		// B2 (RF-1.2): un empleado de rol acotado solo opera en las sucursales que tiene asignadas.
		if (!asignaciones.empleadoPuedeOperarEn(comando.empresaId(), comando.empleadoId(), comando.sucursalId())) {
			throw new com.costumi.backend.compartido.EmpleadoNoAsignadoASucursal(comando.sucursalId());
		}
		// SEC-2: el cliente debe existir y ser de esta empresa (no una referencia colgante/ajena).
		if (!clientes.existe(comando.empresaId(), comando.clienteId())) {
			throw new IllegalArgumentException("El cliente no existe en esta empresa");
		}
		// B1 (RF-7.4): un cliente en lista negra no puede iniciar una renta.
		if (clientes.estaEnListaNegra(comando.empresaId(), comando.clienteId())) {
			throw new ClienteEnListaNegra(comando.clienteId());
		}
		// Cantidad total pedida por prenda (una misma prenda puede venir en varias líneas).
		Map<UUID, Integer> cantidadPorPrenda = new LinkedHashMap<>();
		for (LineaDeRentaComando linea : comando.lineas()) {
			cantidadPorPrenda.merge(linea.prendaId(), linea.cantidad(), Integer::sum);
		}
		for (UUID prendaId : cantidadPorPrenda.keySet()) {
			if (!inventario.prendaExiste(comando.empresaId(), prendaId)) {
				throw new IllegalArgumentException("La prenda no existe en esta empresa");
			}
		}
		// RF-12.4: la disponibilidad por fechas solo se controla si la empresa cuenta stock.
		if (configuracion.conteoStock(comando.empresaId())) {
			for (Map.Entry<UUID, Integer> pedido : cantidadPorPrenda.entrySet()) {
				UUID prendaId = pedido.getKey();
				// Serializa las reservas de esta prenda (evita doble asignación) antes de contar disponibilidad.
				rentas.bloquearReservaDePrenda(prendaId);
				int disponibles = inventario.unidadesDisponibles(comando.empresaId(), comando.sucursalId(), prendaId);
				long ocupadas = rentas.cantidadSolapada(comando.empresaId(), prendaId,
						comando.fechaRetiro(), comando.fechaDevolucion());
				if (ocupadas + pedido.getValue() > disponibles) {
					throw new SinDisponibilidad();
				}
			}
		}
		List<RentaLinea> lineas = comando.lineas().stream()
				.map(l -> RentaLinea.de(l.prendaId(), l.cantidad(), l.precioPorDia()))
				.toList();
		return rentas.guardar(Renta.crear(comando.empresaId(), comando.sucursalId(), comando.clienteId(), lineas,
				comando.fechaRetiro(), comando.fechaDevolucion(), comando.deposito(), comando.claveIdempotencia(),
				comando.empleadoId()));
	}

	@Override
	@Transactional
	public UUID registrar(UUID empresaId, UUID sucursalId, UUID clienteId, java.time.LocalDate fechaRetiro,
			java.time.LocalDate fechaDevolucion, java.math.BigDecimal deposito, List<ItemDeRenta> items,
			UUID empleadoId) {
		List<LineaDeRentaComando> lineas = items.stream()
				.map(i -> new LineaDeRentaComando(i.prendaId(), i.cantidad(), i.precioPorDia()))
				.toList();
		Renta renta = ejecutar(new CrearRentaComando(empresaId, sucursalId, clienteId, lineas, fechaRetiro,
				fechaDevolucion, deposito, null, empleadoId));
		return renta.id();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UUID> prendaDeRenta(UUID empresaId, UUID rentaId) {
		return rentas.buscarPorId(rentaId)
				.filter(renta -> renta.empresaId().equals(empresaId))
				.map(Renta::prendaId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<LineaDeRentaVista> lineasDeRenta(UUID empresaId, UUID rentaId) {
		return rentas.buscarPorId(rentaId)
				.filter(renta -> renta.empresaId().equals(empresaId))
				.map(renta -> renta.lineas().stream()
						.map(linea -> new LineaDeRentaVista(linea.prendaId(), linea.cantidad()))
						.toList())
				.orElseGet(List::of);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UUID> sucursalDeRenta(UUID empresaId, UUID rentaId) {
		return rentas.buscarPorId(rentaId)
				.filter(renta -> renta.empresaId().equals(empresaId))
				.map(Renta::sucursalId);
	}

	@Override
	@Transactional(readOnly = true)
	public int contarRentasVigentesEnSucursal(UUID empresaId, UUID sucursalId) {
		return (int) rentas.listarPorEmpresa(empresaId).stream()
				.filter(renta -> renta.sucursalId().equals(sucursalId))
				.filter(renta -> renta.estado() != EstadoRenta.CERRADA && renta.estado() != EstadoRenta.CANCELADA)
				.count();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UUID> clienteDeRenta(UUID empresaId, UUID rentaId) {
		return rentas.buscarPorId(rentaId)
				.filter(renta -> renta.empresaId().equals(empresaId))
				.map(Renta::clienteId);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<java.math.BigDecimal> importeDeRenta(UUID empresaId, UUID rentaId) {
		return rentas.buscarPorId(rentaId)
				.filter(renta -> renta.empresaId().equals(empresaId))
				.map(Renta::importe);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<java.time.LocalDate> fechaDevolucionDeRenta(UUID empresaId, UUID rentaId) {
		return rentas.buscarPorId(rentaId)
				.filter(renta -> renta.empresaId().equals(empresaId))
				.map(Renta::fechaDevolucion);
	}

	@Override
	@Transactional
	public void marcarDevuelta(UUID empresaId, UUID rentaId) {
		aplicar(empresaId, rentaId, Renta::devolver);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Renta> buscar(UUID empresaId, UUID clienteId) {
		return (clienteId == null) ? rentas.listarPorEmpresa(empresaId)
				: rentas.listarPorCliente(empresaId, clienteId);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Renta> buscarPorId(UUID empresaId, UUID rentaId) {
		return rentas.buscarPorId(rentaId).filter(renta -> renta.empresaId().equals(empresaId));
	}

	@Override
	@Transactional
	public Renta entregar(UUID empresaId, UUID rentaId) {
		return aplicar(empresaId, rentaId, Renta::entregar);
	}

	@Override
	@Transactional
	public Renta devolver(UUID empresaId, UUID rentaId) {
		return aplicar(empresaId, rentaId, Renta::devolver);
	}

	@Override
	@Transactional
	public Renta cerrar(UUID empresaId, UUID rentaId) {
		return aplicar(empresaId, rentaId, Renta::cerrar);
	}

	@Override
	@Transactional
	public Renta cancelar(UUID empresaId, UUID rentaId) {
		return aplicar(empresaId, rentaId, Renta::cancelar);
	}

	@Override
	@Transactional
	public Renta extender(UUID empresaId, UUID rentaId, java.time.LocalDate nuevaFechaDevolucion) {
		return aplicar(empresaId, rentaId, r -> r.extender(nuevaFechaDevolucion));
	}

	private Renta aplicar(UUID empresaId, UUID rentaId, Consumer<Renta> accion) {
		Renta renta = rentas.buscarPorId(rentaId)
				.filter(r -> r.empresaId().equals(empresaId))
				.orElseThrow(() -> new RentaNoEncontrada(rentaId));
		accion.accept(renta);
		return rentas.guardar(renta);
	}
}
