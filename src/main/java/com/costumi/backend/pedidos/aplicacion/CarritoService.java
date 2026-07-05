package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.pedidos.dominio.Carrito;
import com.costumi.backend.pedidos.dominio.CarritoRepository;
import com.costumi.backend.pedidos.dominio.TipoPedido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Casos de uso del carrito, acotados a la empresa (tenant) y segmentados por RF-16. */
@Service
class CarritoService implements AgregarItemAlCarrito, ConsultarCarrito {

	private final CarritoRepository carritos;

	CarritoService(CarritoRepository carritos) {
		this.carritos = carritos;
	}

	@Override
	@Transactional
	public Carrito ejecutar(AgregarItemAlCarritoComando comando) {
		Carrito carrito = carritos
				.buscarPendiente(comando.empresaId(), comando.sucursalId(), comando.clienteId(), comando.tipo())
				.orElseGet(() -> Carrito.crear(
						comando.empresaId(), comando.sucursalId(), comando.clienteId(), comando.tipo()));
		carrito.agregarItem(comando.prendaId(), comando.cantidad());
		return carritos.guardar(carrito);
	}

	@Override
	@Transactional(readOnly = true)
	public Carrito pendiente(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo) {
		return carritos.buscarPendiente(empresaId, sucursalId, clienteId, tipo)
				.orElseThrow(CarritoNoEncontrado::new);
	}
}
