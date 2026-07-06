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

	AuditoriaService(RegistroDeAuditoriaRepository registros) {
		this.registros = registros;
	}

	@Override
	@Transactional
	public void registrar(UUID empresaId, String accion, String detalle) {
		registros.guardar(RegistroDeAuditoria.de(empresaId, accion, detalle));
	}

	@Override
	@Transactional(readOnly = true)
	public List<RegistroDeAuditoria> deEmpresa(UUID empresaId) {
		return registros.listarPorEmpresa(empresaId);
	}
}
