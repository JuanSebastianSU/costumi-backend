package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.ResolucionDeClientes;
import com.costumi.backend.clientes.dominio.Cliente;
import com.costumi.backend.clientes.dominio.ClienteRepository;
import com.costumi.backend.clientes.dominio.FiltroDeClientes;
import com.costumi.backend.clientes.dominio.HistorialItem;
import com.costumi.backend.clientes.dominio.HistorialReadRepository;
import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Casos de uso de Clientes: crear, buscar, lista negra e historial, acotados a la empresa (tenant). */
@Service
class ClienteService implements CrearCliente, ConsultarClientes, CambiarListaNegra, ConsultarHistorial,
		RegistrarDeviceToken, EditarCliente, CambiarEstadoCliente, ResolucionDeClientes {

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
	@Transactional(readOnly = true)
	public Pagina<Cliente> listar(UUID empresaId, String texto, Collection<UUID> idsFiltro,
			boolean incluirArchivados, SolicitudDePagina solicitud) {
		String normalizado = (texto == null || texto.isBlank()) ? null : texto.trim();
		return clientes.listar(empresaId, normalizado, idsFiltro, incluirArchivados, solicitud);
	}

	@Override
	@Transactional
	public Cliente ejecutar(EditarClienteComando comando) {
		Cliente cliente = exigirDelTenant(comando.empresaId(), comando.clienteId());
		cliente.editar(comando.nombre(), comando.telefono(), comando.email(), comando.documento(),
				comando.direccion());
		return clientes.guardar(cliente);
	}

	@Override
	@Transactional
	public Cliente archivar(UUID empresaId, UUID clienteId) {
		Cliente cliente = exigirDelTenant(empresaId, clienteId);
		cliente.archivar();
		return clientes.guardar(cliente);
	}

	@Override
	@Transactional
	public Cliente activar(UUID empresaId, UUID clienteId) {
		Cliente cliente = exigirDelTenant(empresaId, clienteId);
		cliente.activar();
		return clientes.guardar(cliente);
	}

	private Cliente exigirDelTenant(UUID empresaId, UUID clienteId) {
		return clientes.buscarPorId(clienteId)
				.filter(c -> c.empresaId().equals(empresaId))
				.orElseThrow(() -> new ClienteNoEncontrado(clienteId));
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
	public List<UUID> clientesPorFiltro(UUID empresaId, FiltroDeClientes filtro) {
		return historial.clientesPorFiltro(empresaId, filtro, LocalDate.now());
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Map<UUID, com.costumi.backend.clientes.dominio.CargaDeCliente> cargaDeClientes(
			UUID empresaId, java.util.Collection<UUID> clienteIds) {
		return historial.cargaDeClientes(empresaId, clienteIds);
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

	@Override
	@Transactional(readOnly = true)
	public java.util.Optional<UUID> fichaDeUsuarioSiExiste(UUID empresaId, UUID usuarioId) {
		return clientes.buscarPorEmpresaYUsuario(empresaId, usuarioId).map(Cliente::id);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existe(UUID empresaId, UUID clienteId) {
		return clientes.buscarPorId(clienteId)
				.filter(cliente -> empresaId.equals(cliente.empresaId()))
				.isPresent();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean estaEnListaNegra(UUID empresaId, UUID clienteId) {
		return clientes.buscarPorId(clienteId)
				.filter(cliente -> empresaId.equals(cliente.empresaId()))
				.map(Cliente::enListaNegra)
				.orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean estaArchivado(UUID empresaId, UUID clienteId) {
		return clientes.buscarPorId(clienteId)
				.filter(cliente -> empresaId.equals(cliente.empresaId()))
				.map(Cliente::archivada)
				.orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Optional<String> nombreDeCliente(UUID empresaId, UUID clienteId) {
		return clientes.buscarPorId(clienteId)
				.filter(cliente -> empresaId.equals(cliente.empresaId()))
				.map(Cliente::nombre);
	}
}
