package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Empresa;
import com.costumi.backend.identidad.dominio.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orquesta el auto-registro: crea la Empresa (nace PENDIENTE) y la persiste. */
@Service
class RegistrarEmpresaService implements RegistrarEmpresa {

	private final EmpresaRepository empresas;

	RegistrarEmpresaService(EmpresaRepository empresas) {
		this.empresas = empresas;
	}

	@Override
	@Transactional
	public Empresa ejecutar(RegistrarEmpresaComando comando) {
		Empresa empresa = Empresa.registrar(comando.nombre());
		return empresas.guardar(empresa);
	}
}
