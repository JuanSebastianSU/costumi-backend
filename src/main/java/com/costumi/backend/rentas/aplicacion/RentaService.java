package com.costumi.backend.rentas.aplicacion;

import com.costumi.backend.rentas.dominio.Renta;
import com.costumi.backend.rentas.dominio.RentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Casos de uso de Rentas: crear, listar y transiciones de estado, acotados a la empresa (tenant). */
@Service
class RentaService implements CrearRenta, ConsultarRentas, GestionarRenta {

	private final RentaRepository rentas;

	RentaService(RentaRepository rentas) {
		this.rentas = rentas;
	}

	@Override
	@Transactional
	public Renta ejecutar(CrearRentaComando comando) {
		return rentas.guardar(Renta.crear(comando.empresaId(), comando.sucursalId(), comando.clienteId(),
				comando.prendaId(), comando.fechaRetiro(), comando.fechaDevolucion(), comando.precioPorDia(),
				comando.deposito()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Renta> buscar(UUID empresaId, UUID clienteId) {
		return (clienteId == null) ? rentas.listarPorEmpresa(empresaId)
				: rentas.listarPorCliente(empresaId, clienteId);
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

	private Renta aplicar(UUID empresaId, UUID rentaId, Consumer<Renta> accion) {
		Renta renta = rentas.buscarPorId(rentaId)
				.filter(r -> r.empresaId().equals(empresaId))
				.orElseThrow(() -> new RentaNoEncontrada(rentaId));
		accion.accept(renta);
		return rentas.guardar(renta);
	}
}
