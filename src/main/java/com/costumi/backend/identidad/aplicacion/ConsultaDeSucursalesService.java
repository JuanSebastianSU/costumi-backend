package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.ConsultaDeSucursales;
import com.costumi.backend.identidad.dominio.SucursalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Implementa la API pública {@link ConsultaDeSucursales}: valida referencias a sucursal por tenant. */
@Service
class ConsultaDeSucursalesService implements ConsultaDeSucursales {

	private final SucursalRepository sucursales;

	ConsultaDeSucursalesService(SucursalRepository sucursales) {
		this.sucursales = sucursales;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existeActiva(UUID empresaId, UUID sucursalId) {
		return sucursales.buscarPorId(sucursalId)
				.filter(sucursal -> empresaId.equals(sucursal.empresaId()))
				.filter(sucursal -> !sucursal.archivada())
				.isPresent();
	}
}
