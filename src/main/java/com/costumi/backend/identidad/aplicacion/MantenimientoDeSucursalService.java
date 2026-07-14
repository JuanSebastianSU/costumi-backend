package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.DependenciasDeSucursal;
import com.costumi.backend.identidad.dominio.Sucursal;
import com.costumi.backend.identidad.dominio.SucursalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Mantenimiento del ciclo de vida de la Sucursal (RF-15.1): editar sus datos y archivar/reactivar.
 *
 * <p><b>Integridad al archivar:</b> una sucursal no se puede archivar mientras conserve dependencias
 * operativas — unidades de stock o rentas vigentes —; en ese caso se lanza {@link SucursalConDependencias}
 * (→409) con los conteos. Esos datos vienen del puerto {@link DependenciasDeSucursal}: Identidad no depende
 * de Inventario/Rentas (evitaría un ciclo de módulos); el módulo {@code operaciones} implementa el puerto.
 *
 * <p><i>Nota:</i> el guard impide dejar huérfano el inventario/las obligaciones abiertas al archivar. No se
 * valida "sucursal activa" al <i>crear</i> stock/rentas nuevos, porque eso exigiría que Inventario/Rentas
 * dependieran de Identidad (cerraría un ciclo): queda como decisión abierta documentada.
 */
@Service
class MantenimientoDeSucursalService implements EditarSucursal, GestionarEstadoDeSucursal {

	private final SucursalRepository sucursales;
	private final DependenciasDeSucursal dependencias;

	MantenimientoDeSucursalService(SucursalRepository sucursales, DependenciasDeSucursal dependencias) {
		this.sucursales = sucursales;
		this.dependencias = dependencias;
	}

	@Override
	@Transactional
	public Sucursal ejecutar(EditarSucursalComando comando) {
		Sucursal sucursal = sucursalDelTenant(comando.empresaId(), comando.sucursalId());
		sucursal.editar(comando.nombre(), comando.direccion(), comando.ubicacionMaps());
		return sucursales.guardar(sucursal);
	}

	@Override
	@Transactional
	public Sucursal archivar(UUID empresaId, UUID sucursalId) {
		Sucursal sucursal = sucursalDelTenant(empresaId, sucursalId);
		DependenciasDeSucursal.Conteo conteo = dependencias.contar(empresaId, sucursalId);
		if (conteo.hayDependencias()) {
			throw new SucursalConDependencias(sucursalId, conteo.unidadesStock(), conteo.rentasVigentes());
		}
		sucursal.archivar();
		return sucursales.guardar(sucursal);
	}

	@Override
	@Transactional
	public Sucursal activar(UUID empresaId, UUID sucursalId) {
		Sucursal sucursal = sucursalDelTenant(empresaId, sucursalId);
		sucursal.activar();
		return sucursales.guardar(sucursal);
	}

	/** Carga la sucursal y exige que pertenezca a la empresa del token (aislamiento por tenant, §5.4). */
	private Sucursal sucursalDelTenant(UUID empresaId, UUID sucursalId) {
		return sucursales.buscarPorId(sucursalId)
				.filter(s -> empresaId.equals(s.empresaId()))
				.orElseThrow(() -> new SucursalNoEncontrada(sucursalId));
	}
}
