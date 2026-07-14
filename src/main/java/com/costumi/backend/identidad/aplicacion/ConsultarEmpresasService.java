package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Empresa;
import com.costumi.backend.identidad.dominio.EmpresaRepository;
import com.costumi.backend.identidad.dominio.EstadoEmpresa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lista las Empresas ACTIVAS y SUSPENDIDAS para el panel del SuperAdmin, ordenadas por nombre.
 * Es la lista desde la que se ejerce suspender/reactivar (RF-15.3).
 */
@Service
class ConsultarEmpresasService implements ConsultarEmpresas {

	private final EmpresaRepository empresas;

	ConsultarEmpresasService(EmpresaRepository empresas) {
		this.empresas = empresas;
	}

	@Override
	@Transactional(readOnly = true)
	public List<EmpresaVista> ejecutar() {
		return Stream.concat(
						empresas.listarPorEstado(EstadoEmpresa.ACTIVA).stream(),
						empresas.listarPorEstado(EstadoEmpresa.SUSPENDIDA).stream())
				.sorted(Comparator.comparing(Empresa::nombre, String.CASE_INSENSITIVE_ORDER))
				.map(empresa -> new EmpresaVista(empresa.id(), empresa.nombre(), empresa.estado(),
						empresa.fechaRegistro(), empresa.ubicacion(), empresa.contacto()))
				.toList();
	}
}
