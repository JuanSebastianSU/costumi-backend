package com.costumi.backend.ventas.aplicacion;

import com.costumi.backend.configuracion.ConsultaDeConfiguracion;
import com.costumi.backend.inventario.AjusteDeInventario;
import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.ventas.ConsultaDeVentas;
import com.costumi.backend.ventas.RegistroDeVentas;
import com.costumi.backend.ventas.dominio.LineaDeVenta;
import com.costumi.backend.ventas.dominio.Venta;
import com.costumi.backend.ventas.dominio.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Casos de uso de Ventas, acotados a la empresa (tenant). */
@Service
class VentaService implements RegistrarVenta, ConsultarVentas, RegistroDeVentas, ConsultaDeVentas {

	private final VentaRepository ventas;
	private final ConsultaDeInventario inventario;
	private final AjusteDeInventario ajusteDeInventario;
	private final ConsultaDeConfiguracion configuracion;

	VentaService(VentaRepository ventas, ConsultaDeInventario inventario, AjusteDeInventario ajusteDeInventario,
			ConsultaDeConfiguracion configuracion) {
		this.ventas = ventas;
		this.inventario = inventario;
		this.ajusteDeInventario = ajusteDeInventario;
		this.configuracion = configuracion;
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
		// RF-12.4: solo se descuenta stock si la empresa cuenta stock; si no, la venta no toca inventario.
		if (configuracion.conteoStock(comando.empresaId())) {
			// Baja de stock al confirmar (RF-4.4): si no alcanza, StockInsuficiente revierte toda la venta.
			for (LineaDeVenta linea : comando.lineas()) {
				ajusteDeInventario.descontarDisponibles(comando.empresaId(), comando.sucursalId(), linea.prendaId(), linea.cantidad());
			}
		}
		return ventas.guardar(venta);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Venta> deEmpresa(UUID empresaId) {
		return ventas.listarPorEmpresa(empresaId);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<BigDecimal> totalDeVenta(UUID empresaId, UUID ventaId) {
		return ventas.buscarPorId(ventaId)
				.filter(venta -> venta.empresaId().equals(empresaId))
				.map(Venta::total);
	}

	@Override
	@Transactional
	public UUID registrar(UUID empresaId, UUID sucursalId, UUID empleadoId, UUID clienteId, List<ItemDeVenta> items) {
		List<LineaDeVenta> lineas = items.stream()
				.map(i -> LineaDeVenta.de(i.prendaId(), i.cantidad(), i.precioUnitario()))
				.toList();
		Venta venta = ejecutar(new RegistrarVentaComando(empresaId, sucursalId, empleadoId, clienteId,
				BigDecimal.ZERO, lineas));
		return venta.id();
	}
}
