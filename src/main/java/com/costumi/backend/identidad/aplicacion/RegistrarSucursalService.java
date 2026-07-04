package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Empresa;
import com.costumi.backend.identidad.dominio.EmpresaRepository;
import com.costumi.backend.identidad.dominio.Sucursal;
import com.costumi.backend.identidad.dominio.SucursalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Da de alta una Sucursal. Regla RF-15.4: solo una Empresa ACTIVA puede abrir sucursales
 * (una PENDIENTE/SUSPENDIDA/RECHAZADA no puede configurar ni operar).
 */
@Service
class RegistrarSucursalService implements RegistrarSucursal {

	private final EmpresaRepository empresas;
	private final SucursalRepository sucursales;

	RegistrarSucursalService(EmpresaRepository empresas, SucursalRepository sucursales) {
		this.empresas = empresas;
		this.sucursales = sucursales;
	}

	@Override
	@Transactional
	public Sucursal ejecutar(RegistrarSucursalComando comando) {
		Empresa empresa = empresas.buscarPorId(comando.empresaId())
				.orElseThrow(() -> new EmpresaNoEncontrada(comando.empresaId()));
		if (!empresa.estado().esOperativa()) {
			throw new EmpresaNoOperativa(empresa.id(), empresa.estado());
		}
		Sucursal sucursal = Sucursal.crear(empresa.id(), comando.nombre(), comando.direccion());
		return sucursales.guardar(sucursal);
	}
}
