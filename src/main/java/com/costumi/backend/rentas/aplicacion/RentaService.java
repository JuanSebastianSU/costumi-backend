package com.costumi.backend.rentas.aplicacion;

import com.costumi.backend.configuracion.ConsultaDeConfiguracion;
import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.rentas.ConsultaDeRentas;
import com.costumi.backend.rentas.dominio.Renta;
import com.costumi.backend.rentas.dominio.RentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/** Casos de uso de Rentas: crear, listar y transiciones de estado, acotados a la empresa (tenant). */
@Service
class RentaService implements CrearRenta, ConsultarRentas, GestionarRenta, ConsultaDeRentas {

	private final RentaRepository rentas;
	private final ConsultaDeInventario inventario;
	private final ConsultaDeConfiguracion configuracion;

	RentaService(RentaRepository rentas, ConsultaDeInventario inventario, ConsultaDeConfiguracion configuracion) {
		this.rentas = rentas;
		this.inventario = inventario;
		this.configuracion = configuracion;
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
		if (!inventario.prendaExiste(comando.empresaId(), comando.prendaId())) {
			throw new IllegalArgumentException("La prenda no existe en esta empresa");
		}
		// RF-12.4: la disponibilidad por fechas solo se controla si la empresa cuenta stock.
		if (configuracion.conteoStock(comando.empresaId())) {
			// Serializa las reservas de esta prenda (evita doble asignación) antes de contar disponibilidad.
			rentas.bloquearReservaDePrenda(comando.prendaId());
			int disponibles = inventario.unidadesDisponibles(comando.empresaId(), comando.sucursalId(), comando.prendaId());
			long ocupadas = rentas.contarSolapadas(comando.empresaId(), comando.prendaId(),
					comando.fechaRetiro(), comando.fechaDevolucion());
			if (ocupadas >= disponibles) {
				throw new SinDisponibilidad();
			}
		}
		return rentas.guardar(Renta.crear(comando.empresaId(), comando.sucursalId(), comando.clienteId(),
				comando.prendaId(), comando.fechaRetiro(), comando.fechaDevolucion(), comando.precioPorDia(),
				comando.deposito(), comando.claveIdempotencia()));
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
	public Optional<UUID> sucursalDeRenta(UUID empresaId, UUID rentaId) {
		return rentas.buscarPorId(rentaId)
				.filter(renta -> renta.empresaId().equals(empresaId))
				.map(Renta::sucursalId);
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
