package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Empresa;
import com.costumi.backend.identidad.dominio.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Devuelve la empresa del token. Si no existe es un token de una empresa borrada: 404, no 500. */
@Service
class ConsultarMiEmpresaService implements ConsultarMiEmpresa {

	private final EmpresaRepository empresas;

	ConsultarMiEmpresaService(EmpresaRepository empresas) {
		this.empresas = empresas;
	}

	@Override
	@Transactional(readOnly = true)
	public Empresa ejecutar(UUID empresaId) {
		return empresas.buscarPorId(empresaId).orElseThrow(() -> new EmpresaNoEncontrada(empresaId));
	}
}
