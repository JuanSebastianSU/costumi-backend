package com.costumi.backend.rentas.adaptadores.salida;

import com.costumi.backend.rentas.dominio.Renta;
import com.costumi.backend.rentas.dominio.RentaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link RentaRepository} con JPA. */
@Repository
class RentaRepositoryAdapter implements RentaRepository {

	private final RentaJpaRepository jpa;

	RentaRepositoryAdapter(RentaJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Renta guardar(Renta renta) {
		return aDominio(jpa.save(aEntidad(renta)));
	}

	@Override
	public Optional<Renta> buscarPorId(UUID id) {
		return jpa.findFirstById(id).map(RentaRepositoryAdapter::aDominio);
	}

	@Override
	public List<Renta> listarPorEmpresa(UUID empresaId) {
		return jpa.findByEmpresaId(empresaId).stream().map(RentaRepositoryAdapter::aDominio).toList();
	}

	@Override
	public List<Renta> listarPorCliente(UUID empresaId, UUID clienteId) {
		return jpa.findByEmpresaIdAndClienteId(empresaId, clienteId).stream()
				.map(RentaRepositoryAdapter::aDominio).toList();
	}

	private static RentaJpaEntity aEntidad(Renta r) {
		return new RentaJpaEntity(r.id(), r.empresaId(), r.sucursalId(), r.clienteId(), r.prendaId(),
				r.fechaRetiro(), r.fechaDevolucion(), r.precioPorDia(), r.deposito(), r.importe(), r.estado());
	}

	private static Renta aDominio(RentaJpaEntity e) {
		return Renta.rehidratar(e.getId(), e.getEmpresaId(), e.getSucursalId(), e.getClienteId(), e.getPrendaId(),
				e.getFechaRetiro(), e.getFechaDevolucion(), e.getPrecioPorDia(), e.getDeposito(), e.getImporte(),
				e.getEstado());
	}
}
