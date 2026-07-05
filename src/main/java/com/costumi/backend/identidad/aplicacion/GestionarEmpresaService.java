package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.EmpresaAprobada;
import com.costumi.backend.identidad.dominio.Empresa;
import com.costumi.backend.identidad.dominio.EmpresaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

/** Carga la Empresa, aplica la transición de dominio (que valida la legalidad) y la guarda. */
@Service
class GestionarEmpresaService implements GestionarEmpresa {

	private final EmpresaRepository empresas;
	private final ApplicationEventPublisher eventos;

	GestionarEmpresaService(EmpresaRepository empresas, ApplicationEventPublisher eventos) {
		this.empresas = empresas;
		this.eventos = eventos;
	}

	@Override
	@Transactional
	public Empresa aprobar(UUID id) {
		Empresa empresa = aplicar(id, Empresa::aprobar);
		eventos.publishEvent(new EmpresaAprobada(empresa.id()));
		return empresa;
	}

	@Override
	@Transactional
	public Empresa rechazar(UUID id) {
		return aplicar(id, Empresa::rechazar);
	}

	@Override
	@Transactional
	public Empresa suspender(UUID id) {
		return aplicar(id, Empresa::suspender);
	}

	@Override
	@Transactional
	public Empresa reactivar(UUID id) {
		return aplicar(id, Empresa::reactivar);
	}

	private Empresa aplicar(UUID id, Consumer<Empresa> accion) {
		Empresa empresa = empresas.buscarPorId(id).orElseThrow(() -> new EmpresaNoEncontrada(id));
		accion.accept(empresa);
		return empresas.guardar(empresa);
	}
}
