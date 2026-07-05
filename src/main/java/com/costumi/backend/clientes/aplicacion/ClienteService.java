package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.dominio.Cliente;
import com.costumi.backend.clientes.dominio.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Casos de uso de Clientes: crear, buscar y lista negra, acotados a la empresa (tenant). */
@Service
class ClienteService implements CrearCliente, ConsultarClientes, CambiarListaNegra {

	private final ClienteRepository clientes;

	ClienteService(ClienteRepository clientes) {
		this.clientes = clientes;
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
}
