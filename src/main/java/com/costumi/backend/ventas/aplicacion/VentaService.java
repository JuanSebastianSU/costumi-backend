package com.costumi.backend.ventas.aplicacion;

import com.costumi.backend.ventas.dominio.Venta;
import com.costumi.backend.ventas.dominio.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Casos de uso de Ventas, acotados a la empresa (tenant). */
@Service
class VentaService implements RegistrarVenta, ConsultarVentas {

	private final VentaRepository ventas;

	VentaService(VentaRepository ventas) {
		this.ventas = ventas;
	}

	@Override
	@Transactional
	public Venta ejecutar(RegistrarVentaComando comando) {
		return ventas.guardar(Venta.crear(comando.empresaId(), comando.sucursalId(), comando.empleadoId(),
				comando.clienteId(), comando.descuento(), comando.lineas()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Venta> deEmpresa(UUID empresaId) {
		return ventas.listarPorEmpresa(empresaId);
	}
}
