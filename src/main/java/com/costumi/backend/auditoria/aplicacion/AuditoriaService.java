package com.costumi.backend.auditoria.aplicacion;

import com.costumi.backend.auditoria.dominio.RegistroDeAuditoria;
import com.costumi.backend.auditoria.dominio.RegistroDeAuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Casos de uso de auditoría, acotados a la empresa (tenant). */
@Service
class AuditoriaService implements RegistrarAuditoria, ConsultarAuditoria {

	private final RegistroDeAuditoriaRepository registros;
	private final com.costumi.backend.compartido.ContextoDeTenant tenant;

	AuditoriaService(RegistroDeAuditoriaRepository registros,
			com.costumi.backend.compartido.ContextoDeTenant tenant) {
		this.registros = registros;
		this.tenant = tenant;
	}

	@Override
	@Transactional
	public void registrar(UUID empresaId, String accion, String detalle) {
		// El «quién»: el usuario autenticado de la petición. Los listeners de auditoría corren AFTER_COMMIT
		// en el mismo hilo, así que el SecurityContext sigue disponible; en jobs sin sesión queda null.
		UUID actor = tenant.usuarioId().orElse(null);
		registros.guardar(RegistroDeAuditoria.de(empresaId, actor, accion, detalle));
	}

	@Override
	@Transactional(readOnly = true)
	public List<RegistroDeAuditoria> deEmpresa(UUID empresaId) {
		return registros.listarPorEmpresa(empresaId);
	}

	@Override
	@Transactional(readOnly = true)
	public com.costumi.backend.compartido.Pagina<RegistroDeAuditoria> deEmpresa(UUID empresaId, String buscar, String tipo, com.costumi.backend.compartido.SolicitudDePagina pagina) {
		return registros.listarPorEmpresa(empresaId, buscar, tipo, pagina);
	}
}
