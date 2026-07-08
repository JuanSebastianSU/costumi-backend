package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.EmpresaAprobada;
import com.costumi.backend.identidad.EmpresaGestionada;
import com.costumi.backend.identidad.dominio.Empresa;
import com.costumi.backend.identidad.dominio.EmpresaRepository;
import com.costumi.backend.identidad.dominio.Sucursal;
import com.costumi.backend.identidad.dominio.SucursalRepository;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

/** Carga la Empresa, aplica la transición de dominio (que valida la legalidad) y la guarda. */
@Service
class GestionarEmpresaService implements GestionarEmpresa {

	private final EmpresaRepository empresas;
	private final SucursalRepository sucursales;
	private final UsuarioRepository usuarios;
	private final ApplicationEventPublisher eventos;

	GestionarEmpresaService(EmpresaRepository empresas, SucursalRepository sucursales, UsuarioRepository usuarios,
			ApplicationEventPublisher eventos) {
		this.empresas = empresas;
		this.sucursales = sucursales;
		this.usuarios = usuarios;
		this.eventos = eventos;
	}

	/**
	 * Aprueba la empresa y, si vino de una solicitud del marketplace (tiene solicitante), la deja lista
	 * para operar: crea su sucursal "Casa Matriz" y <b>promueve al cliente solicitante a DUEÑO</b> de la
	 * empresa (misma cuenta). Así, al re-loguearse, el cliente ve su nueva tienda ("Mi Local").
	 */
	@Override
	@Transactional
	public Empresa aprobar(UUID id) {
		Empresa empresa = aplicar(id, Empresa::aprobar);
		provisionarSiVieneDeSolicitud(empresa);
		eventos.publishEvent(new EmpresaAprobada(empresa.id()));
		return empresa;
	}

	private void provisionarSiVieneDeSolicitud(Empresa empresa) {
		UUID solicitanteId = empresa.solicitanteId();
		if (solicitanteId == null) {
			return; // registro clásico sin cliente: no se provisiona nada (comportamiento previo)
		}
		sucursales.guardar(Sucursal.crear(empresa.id(), "Casa Matriz", empresa.ubicacion()));
		usuarios.buscarPorId(solicitanteId)
				.filter(u -> u.rol().esCliente())
				.ifPresent(cliente -> usuarios.guardar(cliente.promoverADueno(empresa.id())));
	}

	@Override
	@Transactional
	public Empresa rechazar(UUID id) {
		return aplicarYAuditar(id, Empresa::rechazar, "RECHAZADA");
	}

	@Override
	@Transactional
	public Empresa suspender(UUID id) {
		return aplicarYAuditar(id, Empresa::suspender, "SUSPENDIDA");
	}

	@Override
	@Transactional
	public Empresa reactivar(UUID id) {
		return aplicarYAuditar(id, Empresa::reactivar, "REACTIVADA");
	}

	/** Aplica la transición y publica el evento para que Auditoría deje traza de la acción (RF-15.5). */
	private Empresa aplicarYAuditar(UUID id, Consumer<Empresa> accion, String nombreAccion) {
		Empresa empresa = aplicar(id, accion);
		eventos.publishEvent(new EmpresaGestionada(empresa.id(), nombreAccion));
		return empresa;
	}

	private Empresa aplicar(UUID id, Consumer<Empresa> accion) {
		Empresa empresa = empresas.buscarPorId(id).orElseThrow(() -> new EmpresaNoEncontrada(id));
		accion.accept(empresa);
		return empresas.guardar(empresa);
	}
}
