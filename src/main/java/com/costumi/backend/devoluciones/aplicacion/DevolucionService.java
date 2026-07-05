package com.costumi.backend.devoluciones.aplicacion;

import com.costumi.backend.devoluciones.dominio.Devolucion;
import com.costumi.backend.devoluciones.dominio.DevolucionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Casos de uso de Devoluciones, acotados a la empresa (tenant). */
@Service
class DevolucionService implements RegistrarDevolucion, ConsultarDevoluciones {

	private final DevolucionRepository devoluciones;

	DevolucionService(DevolucionRepository devoluciones) {
		this.devoluciones = devoluciones;
	}

	@Override
	@Transactional
	public Devolucion ejecutar(RegistrarDevolucionComando comando) {
		return devoluciones.guardar(Devolucion.crear(comando.empresaId(), comando.rentaId(), comando.deposito(),
				comando.cargoPorDanos(), comando.cargoPorRetraso(), comando.piezas()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Devolucion> deEmpresa(UUID empresaId) {
		return devoluciones.listarPorEmpresa(empresaId);
	}
}
