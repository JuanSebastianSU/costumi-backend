package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.configuracion.ConsultaDeConfiguracion;
import com.costumi.backend.identidad.dominio.Empresa;
import com.costumi.backend.identidad.dominio.EmpresaRepository;
import com.costumi.backend.identidad.dominio.Sucursal;
import com.costumi.backend.identidad.dominio.SucursalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Da de alta una Sucursal. Regla RF-15.4: solo una Empresa ACTIVA puede abrir sucursales
 * (una PENDIENTE/SUSPENDIDA/RECHAZADA no puede configurar ni operar). Además, el interruptor
 * <b>multi-sucursal</b> (RF-12.4) controla de verdad: si está apagado, solo se permite una sucursal.
 */
@Service
class RegistrarSucursalService implements RegistrarSucursal {

	private final EmpresaRepository empresas;
	private final SucursalRepository sucursales;
	private final ConsultaDeConfiguracion configuracion;

	RegistrarSucursalService(EmpresaRepository empresas, SucursalRepository sucursales,
			ConsultaDeConfiguracion configuracion) {
		this.empresas = empresas;
		this.sucursales = sucursales;
		this.configuracion = configuracion;
	}

	@Override
	@Transactional
	public Sucursal ejecutar(RegistrarSucursalComando comando) {
		Empresa empresa = empresas.buscarPorId(comando.empresaId())
				.orElseThrow(() -> new EmpresaNoEncontrada(comando.empresaId()));
		if (!empresa.estado().esOperativa()) {
			throw new EmpresaNoOperativa(empresa.id(), empresa.estado());
		}
		// RF-12.4: sin multi-sucursal, no se puede abrir una segunda sucursal.
		if (!configuracion.multiSucursal(empresa.id()) && !sucursales.listarPorEmpresa(empresa.id()).isEmpty()) {
			throw new LimiteDeSucursales(empresa.id());
		}
		Sucursal sucursal = Sucursal.crear(empresa.id(), comando.nombre(), comando.direccion(),
				comando.ubicacionMaps());
		return sucursales.guardar(sucursal);
	}
}
