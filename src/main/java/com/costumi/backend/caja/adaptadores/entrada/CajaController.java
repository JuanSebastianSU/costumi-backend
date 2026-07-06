package com.costumi.backend.caja.adaptadores.entrada;

import com.costumi.backend.caja.aplicacion.AbrirTurno;
import com.costumi.backend.caja.aplicacion.AbrirTurnoComando;
import com.costumi.backend.caja.aplicacion.CerrarTurno;
import com.costumi.backend.caja.aplicacion.ConsultarTurnos;
import com.costumi.backend.caja.aplicacion.RegistrarMovimiento;
import com.costumi.backend.caja.aplicacion.RegistrarMovimientoComando;
import com.costumi.backend.caja.dominio.Turno;
import com.costumi.backend.compartido.AccesoSinEmpresa;
import com.costumi.backend.compartido.ContextoDeTenant;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Caja (RF-6.3/6.10): apertura de turno, movimientos y cierre con corte/cuadre, acotado al tenant. */
@RestController
@RequestMapping("/api/v1/caja/turnos")
class CajaController {

	private final AbrirTurno abrirTurno;
	private final RegistrarMovimiento registrarMovimiento;
	private final CerrarTurno cerrarTurno;
	private final ConsultarTurnos consultarTurnos;
	private final ContextoDeTenant tenant;

	CajaController(AbrirTurno abrirTurno, RegistrarMovimiento registrarMovimiento, CerrarTurno cerrarTurno,
			ConsultarTurnos consultarTurnos, ContextoDeTenant tenant) {
		this.abrirTurno = abrirTurno;
		this.registrarMovimiento = registrarMovimiento;
		this.cerrarTurno = cerrarTurno;
		this.consultarTurnos = consultarTurnos;
		this.tenant = tenant;
	}

	@PostMapping
	ResponseEntity<TurnoResponse> abrir(@Valid @RequestBody AbrirTurnoRequest request, UriComponentsBuilder uriBuilder) {
		Turno turno = abrirTurno.ejecutar(new AbrirTurnoComando(tenant.empresaIdRequerida(), request.sucursalId(),
				empleado(), request.fondoInicial()));
		URI location = uriBuilder.path("/api/v1/caja/turnos/{id}").buildAndExpand(turno.id()).toUri();
		return ResponseEntity.created(location).body(TurnoResponse.desde(turno));
	}

	@PostMapping("/{turnoId}/movimientos")
	TurnoResponse mover(@PathVariable UUID turnoId, @Valid @RequestBody RegistrarMovimientoRequest request) {
		Turno turno = registrarMovimiento.ejecutar(new RegistrarMovimientoComando(tenant.empresaIdRequerida(), turnoId,
				request.tipo(), request.concepto(), request.monto(), request.metodo()));
		return TurnoResponse.desde(turno);
	}

	@PostMapping("/{turnoId}/cerrar")
	TurnoResponse cerrar(@PathVariable UUID turnoId, @Valid @RequestBody CerrarTurnoRequest request) {
		Turno turno = cerrarTurno.ejecutar(tenant.empresaIdRequerida(), turnoId, request.efectivoContado());
		return TurnoResponse.desde(turno);
	}

	@GetMapping
	List<TurnoResponse> listar() {
		return tenant.empresaId()
				.map(empresaId -> consultarTurnos.deEmpresa(empresaId).stream().map(TurnoResponse::desde).toList())
				.orElseGet(List::of);
	}

	private UUID empleado() {
		return tenant.usuarioId().orElseThrow(AccesoSinEmpresa::new);
	}
}
