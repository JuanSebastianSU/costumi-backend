package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Sucursal;
import com.costumi.backend.identidad.dominio.SucursalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Lista las sucursales de una empresa (scoped por tenant, sólo lectura). */
@Service
class ListarSucursalesService implements ListarSucursales {

	private final SucursalRepository sucursales;

	ListarSucursalesService(SucursalRepository sucursales) {
		this.sucursales = sucursales;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Sucursal> deEmpresa(UUID empresaId) {
		return sucursales.listarPorEmpresa(empresaId);
	}
}
