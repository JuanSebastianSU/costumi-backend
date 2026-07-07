package com.costumi.backend.pagos.adaptadores.salida;

import com.costumi.backend.pagos.dominio.IntentoDePago;
import com.costumi.backend.pagos.dominio.IntentoDePagoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link IntentoDePagoRepository} con JPA. */
@Repository
class IntentoDePagoRepositoryAdapter implements IntentoDePagoRepository {

	private final IntentoDePagoJpaRepository jpa;

	IntentoDePagoRepositoryAdapter(IntentoDePagoJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public IntentoDePago guardar(IntentoDePago intento) {
		return aDominio(jpa.save(aEntidad(intento)));
	}

	@Override
	public Optional<IntentoDePago> buscarPorId(UUID id) {
		return jpa.findFirstById(id).map(IntentoDePagoRepositoryAdapter::aDominio);
	}

	private static IntentoDePagoJpaEntity aEntidad(IntentoDePago i) {
		return new IntentoDePagoJpaEntity(i.id(), i.empresaId(), i.sucursalId(), i.empleadoId(), i.tipoConcepto(),
				i.conceptoId(), i.monto(), i.moneda(), i.referenciaExterna(), i.estado(), i.fecha());
	}

	private static IntentoDePago aDominio(IntentoDePagoJpaEntity e) {
		return IntentoDePago.rehidratar(e.getId(), e.getEmpresaId(), e.getSucursalId(), e.getEmpleadoId(),
				e.getTipoConcepto(), e.getConceptoId(), e.getMonto(), e.getMoneda(), e.getReferenciaExterna(),
				e.getEstado(), e.getFecha());
	}
}
