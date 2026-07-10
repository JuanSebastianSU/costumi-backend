package com.costumi.backend.caja.aplicacion;

import com.costumi.backend.caja.dominio.MovimientoDeCaja;
import com.costumi.backend.caja.dominio.Turno;
import com.costumi.backend.caja.dominio.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Casos de uso de Caja (turnos y movimientos), acotados a la empresa (tenant). */
@Service
class TurnoService implements AbrirTurno, RegistrarMovimiento, CerrarTurno, ConsultarTurnos {

	private final TurnoRepository turnos;
	private final com.costumi.backend.identidad.ConsultaDeSucursales sucursales;

	TurnoService(TurnoRepository turnos, com.costumi.backend.identidad.ConsultaDeSucursales sucursales) {
		this.turnos = turnos;
		this.sucursales = sucursales;
	}

	@Override
	@Transactional
	public Turno ejecutar(AbrirTurnoComando comando) {
		// SEC-1: el turno de caja debe abrirse en una sucursal existente, del tenant y activa.
		if (!sucursales.existeActiva(comando.empresaId(), comando.sucursalId())) {
			throw new IllegalArgumentException("La sucursal no existe o está archivada en esta empresa");
		}
		return turnos.guardar(Turno.abrir(comando.empresaId(), comando.sucursalId(), comando.empleadoId(),
				comando.fondoInicial()));
	}

	@Override
	@Transactional
	public Turno ejecutar(RegistrarMovimientoComando comando) {
		Turno turno = delTenant(comando.empresaId(), comando.turnoId());
		turno.registrar(MovimientoDeCaja.de(comando.tipo(), comando.concepto(), comando.monto(), comando.metodo()));
		return turnos.guardar(turno);
	}

	@Override
	@Transactional
	public Turno ejecutar(UUID empresaId, UUID turnoId, BigDecimal efectivoContado) {
		Turno turno = delTenant(empresaId, turnoId);
		turno.cerrar(efectivoContado);
		return turnos.guardar(turno);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Turno> deEmpresa(UUID empresaId) {
		return turnos.listarPorEmpresa(empresaId);
	}

	private Turno delTenant(UUID empresaId, UUID turnoId) {
		return turnos.buscarPorId(turnoId)
				.filter(turno -> turno.empresaId().equals(empresaId))
				.orElseThrow(() -> new TurnoNoEncontrado(turnoId));
	}
}
