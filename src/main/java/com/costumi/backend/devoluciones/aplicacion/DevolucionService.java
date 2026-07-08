package com.costumi.backend.devoluciones.aplicacion;

import com.costumi.backend.configuracion.ConsultaDeConfiguracion;
import com.costumi.backend.devoluciones.DevolucionRegistrada;
import com.costumi.backend.inventario.AjusteDeInventario;
import com.costumi.backend.devoluciones.dominio.Devolucion;
import com.costumi.backend.devoluciones.dominio.DevolucionRepository;
import com.costumi.backend.devoluciones.dominio.EstadoPieza;
import com.costumi.backend.devoluciones.dominio.PiezaRevisada;
import com.costumi.backend.rentas.ConsultaDeRentas;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Casos de uso de Devoluciones, acotados a la empresa (tenant). Cierra el ciclo de la renta (RF-5). */
@Service
class DevolucionService implements RegistrarDevolucion, ConsultarDevoluciones {

	private final DevolucionRepository devoluciones;
	private final ConsultaDeRentas rentas;
	private final AjusteDeInventario inventario;
	private final ConsultaDeConfiguracion configuracion;
	private final ApplicationEventPublisher eventos;

	DevolucionService(DevolucionRepository devoluciones, ConsultaDeRentas rentas, AjusteDeInventario inventario,
			ConsultaDeConfiguracion configuracion, ApplicationEventPublisher eventos) {
		this.devoluciones = devoluciones;
		this.rentas = rentas;
		this.inventario = inventario;
		this.configuracion = configuracion;
		this.eventos = eventos;
	}

	@Override
	@Transactional
	public Devolucion ejecutar(RegistrarDevolucionComando comando) {
		UUID empresaId = comando.empresaId();
		UUID rentaId = comando.rentaId();
		// La devolución debe apuntar a una renta del tenant (cross-ref §5.4).
		List<ConsultaDeRentas.LineaDeRentaVista> lineas = rentas.lineasDeRenta(empresaId, rentaId);
		if (lineas.isEmpty()) {
			throw new IllegalArgumentException("La renta no existe en esta empresa");
		}
		UUID sucursalId = rentas.sucursalDeRenta(empresaId, rentaId)
				.orElseThrow(() -> new IllegalArgumentException("La renta no existe en esta empresa"));

		// Unidades que ampara la renta, por prenda, y total.
		Map<UUID, Integer> unidadesPorPrenda = new LinkedHashMap<>();
		for (ConsultaDeRentas.LineaDeRentaVista linea : lineas) {
			unidadesPorPrenda.merge(linea.prendaId(), linea.cantidad(), Integer::sum);
		}
		int totalUnidades = unidadesPorPrenda.values().stream().mapToInt(Integer::intValue).sum();

		// Cada pieza revisada debe corresponder a un artículo de la renta (RF-5.6).
		for (PiezaRevisada pieza : comando.piezas()) {
			if (!unidadesPorPrenda.containsKey(pieza.prendaId())) {
				throw new IllegalArgumentException("La pieza revisada no corresponde a ningún artículo de la renta");
			}
		}

		// Lo ya devuelto en devoluciones previas (por prenda) para no exceder ni cerrar antes de tiempo.
		Map<UUID, Integer> yaDevueltoPorPrenda = new HashMap<>();
		int yaDevueltoTotal = 0;
		for (Devolucion previa : devoluciones.listarPorRenta(empresaId, rentaId)) {
			for (PiezaRevisada pieza : previa.piezas()) {
				yaDevueltoPorPrenda.merge(pieza.prendaId(), 1, Integer::sum);
				yaDevueltoTotal++;
			}
		}

		// Unidades revisadas ahora, por prenda; no puede superar lo rentado de cada artículo (RF-5.5).
		Map<UUID, Integer> ahoraPorPrenda = new LinkedHashMap<>();
		for (PiezaRevisada pieza : comando.piezas()) {
			ahoraPorPrenda.merge(pieza.prendaId(), 1, Integer::sum);
		}
		for (Map.Entry<UUID, Integer> entrada : ahoraPorPrenda.entrySet()) {
			int acumulado = yaDevueltoPorPrenda.getOrDefault(entrada.getKey(), 0) + entrada.getValue();
			if (acumulado > unidadesPorPrenda.get(entrada.getKey())) {
				throw new IllegalArgumentException("Se devuelven más unidades de las rentadas para un artículo");
			}
		}

		// Actualiza el inventario por artículo (RF-5.4/5.6): dañadas/limpieza/perdidas salen de disponible.
		for (UUID prendaId : ahoraPorPrenda.keySet()) {
			int danadas = contar(comando.piezas(), prendaId, EstadoPieza.DANADA);
			int enLimpieza = contar(comando.piezas(), prendaId, EstadoPieza.EN_LIMPIEZA);
			int perdidas = contar(comando.piezas(), prendaId, EstadoPieza.PERDIDA);
			if (danadas + enLimpieza + perdidas > 0) {
				inventario.procesarRetornoDeRenta(empresaId, sucursalId, prendaId, danadas, enLimpieza, perdidas);
			}
		}

		// Módulo de multas OFF (RF-6.6/12.4): el recargo por retraso (la multa) no se cobra ni reduce el remanente.
		BigDecimal cargoPorRetraso = configuracion.multasActivas(empresaId)
				? comando.cargoPorRetraso()
				: BigDecimal.ZERO;
		Devolucion devolucion = devoluciones.guardar(Devolucion.crear(empresaId, rentaId,
				comando.deposito(), comando.cargoPorDanos(), cargoPorRetraso, comando.piezas()));

		// Cierra la renta (RF-5.1) SOLO cuando se han devuelto todas las unidades; si no, es parcial (RF-5.5)
		// y la renta sigue ACTIVA para admitir más devoluciones.
		int devueltoTotal = yaDevueltoTotal + comando.piezas().size();
		if (devueltoTotal >= totalUnidades) {
			rentas.marcarDevuelta(empresaId, rentaId);
		}
		// Publica el evento con la multa automática (RF-5.2, §5.5).
		UUID clienteId = rentas.clienteDeRenta(empresaId, rentaId).orElse(null);
		eventos.publishEvent(new DevolucionRegistrada(empresaId, devolucion.id(), rentaId,
				clienteId, devolucion.multa()));
		return devolucion;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Devolucion> deEmpresa(UUID empresaId) {
		return devoluciones.listarPorEmpresa(empresaId);
	}

	private static int contar(List<PiezaRevisada> piezas, UUID prendaId, EstadoPieza estado) {
		return (int) piezas.stream()
				.filter(pieza -> pieza.prendaId().equals(prendaId) && pieza.estado() == estado)
				.count();
	}
}
