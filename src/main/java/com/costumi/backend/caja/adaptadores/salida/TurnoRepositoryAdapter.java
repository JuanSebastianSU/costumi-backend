package com.costumi.backend.caja.adaptadores.salida;

import com.costumi.backend.caja.dominio.MovimientoDeCaja;
import com.costumi.backend.caja.dominio.Turno;
import com.costumi.backend.caja.dominio.TurnoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: persiste el agregado Turno (cabecera + movimientos) con JPA. */
@Repository
class TurnoRepositoryAdapter implements TurnoRepository {

	private final TurnoJpaRepository turnos;
	private final MovimientoCajaJpaRepository movimientos;

	TurnoRepositoryAdapter(TurnoJpaRepository turnos, MovimientoCajaJpaRepository movimientos) {
		this.turnos = turnos;
		this.movimientos = movimientos;
	}

	@Override
	public Turno guardar(Turno turno) {
		turnos.save(new TurnoJpaEntity(turno.id(), turno.empresaId(), turno.sucursalId(), turno.empleadoId(),
				turno.fondoInicial(), turno.estado(), turno.efectivoContado()));
		movimientos.deleteByTurnoId(turno.id());
		int orden = 0;
		for (MovimientoDeCaja movimiento : turno.movimientos()) {
			movimientos.save(new MovimientoCajaJpaEntity(UUID.randomUUID(), turno.id(), turno.empresaId(),
					movimiento.tipo(), movimiento.concepto(), movimiento.monto(), movimiento.metodo(), orden++));
		}
		return turno;
	}

	@Override
	public Optional<Turno> buscarPorId(UUID id) {
		return turnos.findFirstById(id).map(this::aDominio);
	}

	@Override
	public List<Turno> listarPorEmpresa(UUID empresaId) {
		return turnos.findByEmpresaId(empresaId).stream().map(this::aDominio).toList();
	}

	private Turno aDominio(TurnoJpaEntity cabecera) {
		List<MovimientoDeCaja> movimientosDominio = movimientos.findByTurnoIdOrderByOrden(cabecera.getId()).stream()
				.map(m -> MovimientoDeCaja.de(m.getTipo(), m.getConcepto(), m.getMonto(), m.getMetodo()))
				.toList();
		return Turno.rehidratar(cabecera.getId(), cabecera.getEmpresaId(), cabecera.getSucursalId(),
				cabecera.getEmpleadoId(), cabecera.getFondoInicial(), cabecera.getEstado(),
				cabecera.getEfectivoContado(), movimientosDominio);
	}
}
