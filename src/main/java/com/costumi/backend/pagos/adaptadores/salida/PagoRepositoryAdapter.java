package com.costumi.backend.pagos.adaptadores.salida;

import com.costumi.backend.pagos.dominio.Pago;
import com.costumi.backend.pagos.dominio.PagoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link PagoRepository} con JPA. */
@Repository
class PagoRepositoryAdapter implements PagoRepository {

	private final PagoJpaRepository jpa;

	PagoRepositoryAdapter(PagoJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Pago guardar(Pago pago) {
		return aDominio(jpa.save(aEntidad(pago)));
	}

	@Override
	public Optional<Pago> buscarPorId(UUID id) {
		return jpa.findFirstById(id).map(PagoRepositoryAdapter::aDominio);
	}

	@Override
	public List<Pago> listarPorConcepto(UUID empresaId, UUID conceptoId) {
		return jpa.findByEmpresaIdAndConceptoId(empresaId, conceptoId).stream()
				.map(PagoRepositoryAdapter::aDominio).toList();
	}

	@Override
	public Optional<Pago> buscarPorClave(UUID empresaId, String claveIdempotencia) {
		return jpa.findByEmpresaIdAndClaveIdempotencia(empresaId, claveIdempotencia).map(PagoRepositoryAdapter::aDominio);
	}

	@Override
	public java.math.BigDecimal saldoNetoPorConcepto(UUID empresaId, UUID conceptoId) {
		return jpa.findByEmpresaIdAndConceptoId(empresaId, conceptoId).stream()
				.map(PagoRepositoryAdapter::aDominio)
				.map(Pago::montoNeto)
				.reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
	}

	private static PagoJpaEntity aEntidad(Pago p) {
		return new PagoJpaEntity(p.id(), p.empresaId(), p.sucursalId(), p.empleadoId(), p.tipoConcepto(),
				p.conceptoId(), p.monto(), p.tipoPago(), p.metodo(), p.referencia(), p.fecha(), p.claveIdempotencia());
	}

	private static Pago aDominio(PagoJpaEntity e) {
		return Pago.rehidratar(e.getId(), e.getEmpresaId(), e.getSucursalId(), e.getEmpleadoId(), e.getTipoConcepto(),
				e.getConceptoId(), e.getMonto(), e.getTipoPago(), e.getMetodo(), e.getReferencia(), e.getFecha(),
				e.getClaveIdempotencia());
	}
}
