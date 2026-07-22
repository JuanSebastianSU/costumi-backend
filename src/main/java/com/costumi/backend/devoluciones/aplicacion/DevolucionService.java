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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Casos de uso de Devoluciones, acotados a la empresa (tenant). Cierra el ciclo de la renta (RF-5). */
@Service
class DevolucionService implements RegistrarDevolucion, ConsultarDevoluciones,
		com.costumi.backend.devoluciones.ConsultaDeMultas {

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

		// Solo las piezas RESUELTAS consumen unidad y cierran la renta (RF-5.5): una faltante/perdida sin
		// cobrar se registra en el checklist pero deja la unidad pendiente (no cuenta ni mueve inventario).
		Map<UUID, Integer> yaDevueltoPorPrenda = new HashMap<>();
		int yaDevueltoTotal = 0;
		for (Devolucion previa : devoluciones.listarPorRenta(empresaId, rentaId)) {
			for (PiezaRevisada pieza : previa.piezas()) {
				if (!pieza.estaResuelta()) {
					continue;
				}
				yaDevueltoPorPrenda.merge(pieza.prendaId(), 1, Integer::sum);
				yaDevueltoTotal++;
			}
		}

		// Unidades resueltas ahora, por prenda; no puede superar lo rentado de cada artículo (RF-5.5).
		Map<UUID, Integer> ahoraPorPrenda = new LinkedHashMap<>();
		int resueltasAhora = 0;
		for (PiezaRevisada pieza : comando.piezas()) {
			if (!pieza.estaResuelta()) {
				continue;
			}
			ahoraPorPrenda.merge(pieza.prendaId(), 1, Integer::sum);
			resueltasAhora++;
		}
		for (Map.Entry<UUID, Integer> entrada : ahoraPorPrenda.entrySet()) {
			int acumulado = yaDevueltoPorPrenda.getOrDefault(entrada.getKey(), 0) + entrada.getValue();
			if (acumulado > unidadesPorPrenda.get(entrada.getKey())) {
				throw new IllegalArgumentException("Se devuelven más unidades de las rentadas para un artículo");
			}
		}

		// Actualiza el inventario por artículo (RF-5.4/5.6), solo si la empresa cuenta stock: las piezas que
		// vuelven BIEN se liberan (rentada -> disponible); dañadas/limpieza/perdidas van de rentada a ese estado.
		if (configuracion.conteoStock(empresaId)) {
			for (UUID prendaId : ahoraPorPrenda.keySet()) {
				int bien = contar(comando.piezas(), prendaId, EstadoPieza.BIEN);
				int danadas = contar(comando.piezas(), prendaId, EstadoPieza.DANADA);
				int enLimpieza = contar(comando.piezas(), prendaId, EstadoPieza.EN_LIMPIEZA);
				int perdidas = contar(comando.piezas(), prendaId, EstadoPieza.PERDIDA);
				if (bien > 0) {
					inventario.liberarDeRenta(empresaId, sucursalId, prendaId, bien);
				}
				if (danadas + enLimpieza + perdidas > 0) {
					inventario.procesarRetornoDeRenta(empresaId, sucursalId, prendaId, danadas, enLimpieza, perdidas);
				}
			}
		}

		// Módulo de multas (RF-6.6/12.4): apagado ⇒ la devolución NO genera cargos (ni daños ni retraso);
		// encendido ⇒ se cobran los daños indicados y el recargo por retraso (RF-5.2).
		BigDecimal cargoPorDanos;
		BigDecimal cargoPorRetraso;
		if (configuracion.multasActivas(empresaId)) {
			cargoPorDanos = comando.cargoPorDanos() == null ? BigDecimal.ZERO : comando.cargoPorDanos();
			cargoPorRetraso = recargoPorRetraso(comando, empresaId, rentaId);
		} else {
			cargoPorDanos = BigDecimal.ZERO;
			cargoPorRetraso = BigDecimal.ZERO;
		}
		Devolucion devolucion = devoluciones.guardar(Devolucion.crear(empresaId, rentaId,
				comando.deposito(), cargoPorDanos, cargoPorRetraso, comando.piezas()));

		// Cierra la renta (RF-5.1) SOLO cuando se han RESUELTO todas las unidades; si falta alguna (por
		// devolver, o perdida sin cobrar) sigue ACTIVA para admitir más devoluciones (RF-5.5).
		int devueltoTotal = yaDevueltoTotal + resueltasAhora;
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

	@Override
	@Transactional(readOnly = true)
	public com.costumi.backend.compartido.Pagina<Devolucion> deEmpresa(UUID empresaId, String buscar, com.costumi.backend.compartido.SolicitudDePagina pagina) {
		return devoluciones.listarPorEmpresa(empresaId, buscar, pagina);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal totalMultaDeRenta(UUID empresaId, UUID rentaId) {
		return devoluciones.listarPorRenta(empresaId, rentaId).stream()
				.map(Devolucion::multa)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/**
	 * Recargo por retraso (RF-5.2): si el comando trae un valor, es un override manual; si no, se deriva
	 * de la política de la empresa (RF-12.2) según los días de atraso (fecha real − fecha pactada, ≥ 0):
	 * acumulativa (monto × días) o fija (monto único si hubo atraso).
	 */
	private BigDecimal recargoPorRetraso(RegistrarDevolucionComando comando, UUID empresaId, UUID rentaId) {
		if (comando.cargoPorRetraso() != null) {
			return comando.cargoPorRetraso();
		}
		LocalDate pactada = rentas.fechaDevolucionDeRenta(empresaId, rentaId).orElse(null);
		LocalDate real = comando.fechaDevolucionReal();
		if (pactada == null || real == null) {
			return BigDecimal.ZERO;
		}
		long diasAtraso = Math.max(0, ChronoUnit.DAYS.between(pactada, real));
		return configuracion.recargoPorRetraso(empresaId, diasAtraso);
	}

	private static int contar(List<PiezaRevisada> piezas, UUID prendaId, EstadoPieza estado) {
		// Solo piezas resueltas mueven inventario: una perdida solo sale de stock si ya se cobró (RF-5.5/5.6).
		return (int) piezas.stream()
				.filter(pieza -> pieza.estaResuelta() && pieza.prendaId().equals(prendaId) && pieza.estado() == estado)
				.count();
	}
}
