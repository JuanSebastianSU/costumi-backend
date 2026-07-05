package com.costumi.backend.ventas.aplicacion;

import com.costumi.backend.inventario.AjusteDeInventario;
import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.ventas.dominio.LineaDeVenta;
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
	private final ConsultaDeInventario inventario;
	private final AjusteDeInventario ajusteDeInventario;

	VentaService(VentaRepository ventas, ConsultaDeInventario inventario, AjusteDeInventario ajusteDeInventario) {
		this.ventas = ventas;
		this.inventario = inventario;
		this.ajusteDeInventario = ajusteDeInventario;
	}

	@Override
	@Transactional
	public Venta ejecutar(RegistrarVentaComando comando) {
		for (LineaDeVenta linea : comando.lineas()) {
			if (!inventario.prendaExiste(comando.empresaId(), linea.prendaId())) {
				throw new IllegalArgumentException("La prenda no existe en esta empresa");
			}
		}
		Venta venta = Venta.crear(comando.empresaId(), comando.sucursalId(), comando.empleadoId(),
				comando.clienteId(), comando.descuento(), comando.lineas());
		// Baja de stock al confirmar (RF-4.4): si no alcanza, StockInsuficiente revierte toda la venta.
		for (LineaDeVenta linea : comando.lineas()) {
			ajusteDeInventario.descontarDisponibles(comando.empresaId(), linea.prendaId(), linea.cantidad());
		}
		return ventas.guardar(venta);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Venta> deEmpresa(UUID empresaId) {
		return ventas.listarPorEmpresa(empresaId);
	}
}
