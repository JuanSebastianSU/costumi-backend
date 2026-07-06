package com.costumi.backend.devoluciones.aplicacion;

import com.costumi.backend.inventario.AjusteDeInventario;
import com.costumi.backend.devoluciones.dominio.Devolucion;
import com.costumi.backend.devoluciones.dominio.DevolucionRepository;
import com.costumi.backend.devoluciones.dominio.EstadoPieza;
import com.costumi.backend.devoluciones.dominio.PiezaRevisada;
import com.costumi.backend.rentas.ConsultaDeRentas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Casos de uso de Devoluciones, acotados a la empresa (tenant). Cierra el ciclo de la renta (RF-5). */
@Service
class DevolucionService implements RegistrarDevolucion, ConsultarDevoluciones {

	private final DevolucionRepository devoluciones;
	private final ConsultaDeRentas rentas;
	private final AjusteDeInventario inventario;

	DevolucionService(DevolucionRepository devoluciones, ConsultaDeRentas rentas, AjusteDeInventario inventario) {
		this.devoluciones = devoluciones;
		this.rentas = rentas;
		this.inventario = inventario;
	}

	@Override
	@Transactional
	public Devolucion ejecutar(RegistrarDevolucionComando comando) {
		// La devolución debe apuntar a una renta del tenant (cross-ref §5.4).
		UUID prendaId = rentas.prendaDeRenta(comando.empresaId(), comando.rentaId())
				.orElseThrow(() -> new IllegalArgumentException("La renta no existe en esta empresa"));

		// Actualiza el inventario según el checklist (RF-5.4/5.6): dañadas/limpieza/perdidas salen de disponible.
		int danadas = contar(comando.piezas(), EstadoPieza.DANADA);
		int enLimpieza = contar(comando.piezas(), EstadoPieza.EN_LIMPIEZA);
		int perdidas = contar(comando.piezas(), EstadoPieza.PERDIDA);
		inventario.procesarRetornoDeRenta(comando.empresaId(), prendaId, danadas, enLimpieza, perdidas);

		return devoluciones.guardar(Devolucion.crear(comando.empresaId(), comando.rentaId(), comando.deposito(),
				comando.cargoPorDanos(), comando.cargoPorRetraso(), comando.piezas()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Devolucion> deEmpresa(UUID empresaId) {
		return devoluciones.listarPorEmpresa(empresaId);
	}

	private static int contar(List<PiezaRevisada> piezas, EstadoPieza estado) {
		return (int) piezas.stream().filter(pieza -> pieza.estado() == estado).count();
	}
}
