package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.ResolucionDeClientes;
import com.costumi.backend.clientes.dominio.Cliente;
import com.costumi.backend.clientes.dominio.ClienteRepository;
import com.costumi.backend.clientes.dominio.HistorialItem;
import com.costumi.backend.clientes.dominio.HistorialReadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Casos de uso de Clientes: crear, buscar, lista negra e historial, acotados a la empresa (tenant). */
@Service
class ClienteService implements CrearCliente, ConsultarClientes, CambiarListaNegra, ConsultarHistorial,
		RegistrarDeviceToken, ResolucionDeClientes {

	private final ClienteRepository clientes;
	private final HistorialReadRepository historial;

	ClienteService(ClienteRepository clientes, HistorialReadRepository historial) {
		this.clientes = clientes;
		this.historial = historial;
	}

	@Override
	@Transactional
	public Cliente ejecutar(CrearClienteComando comando) {
		return clientes.guardar(Cliente.crear(comando.empresaId(), comando.nombre(), comando.telefono(),
				comando.email(), comando.documento(), comando.direccion()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Cliente> buscar(UUID empresaId, String texto) {
		if (texto == null || texto.isBlank()) {
			return clientes.listarPorEmpresa(empresaId);
		}
		return clientes.buscarPorEmpresaYTexto(empresaId, texto.trim());
	}

	@Override
	@Transactional
	public Cliente ejecutar(CambiarListaNegraComando comando) {
		Cliente cliente = clientes.buscarPorId(comando.clienteId())
				.filter(c -> c.empresaId().equals(comando.empresaId()))
				.orElseThrow(() -> new ClienteNoEncontrado(comando.clienteId()));
		if (comando.enListaNegra()) {
			cliente.ponerEnListaNegra();
		} else {
			cliente.quitarDeListaNegra();
		}
		return clientes.guardar(cliente);
	}

	@Override
	@Transactional
	public Cliente ejecutar(UUID empresaId, UUID clienteId, String deviceToken) {
		Cliente cliente = clientes.buscarPorId(clienteId)
				.filter(c -> c.empresaId().equals(empresaId))
				.orElseThrow(() -> new ClienteNoEncontrado(clienteId));
		cliente.registrarDeviceToken(deviceToken);
		return clientes.guardar(cliente);
	}

	@Override
	@Transactional(readOnly = true)
	public List<HistorialItem> historialDeCliente(UUID empresaId, UUID clienteId) {
		return historial.deCliente(empresaId, clienteId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<UUID> clientesConPendientes(UUID empresaId) {
		return historial.clientesConPendientes(empresaId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<HistorialItem> historialDeUsuario(UUID usuarioId) {
		List<HistorialItem> todo = new java.util.ArrayList<>();
		for (Cliente ficha : clientes.buscarPorUsuario(usuarioId)) {
			todo.addAll(historial.deCliente(ficha.empresaId(), ficha.id()));
		}
		return todo;
	}

	@Override
	@Transactional
	public UUID fichaDeUsuario(UUID empresaId, UUID usuarioId, String email) {
		return clientes.buscarPorEmpresaYUsuario(empresaId, usuarioId)
				.map(Cliente::id)
				.orElseGet(() -> clientes.guardar(Cliente.deUsuario(empresaId, usuarioId, email)).id());
	}
}
