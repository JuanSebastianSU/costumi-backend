package com.costumi.backend.rentas.dominio;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Rentas (scoped por tenant). */
public interface RentaRepository {

	Renta guardar(Renta renta);

	Optional<Renta> buscarPorId(UUID id);

	List<Renta> listarPorEmpresa(UUID empresaId);

	List<Renta> listarPorCliente(UUID empresaId, UUID clienteId);

	/**
	 * Página de rentas de la empresa (opcionalmente por cliente), más recientes primero (C3). {@code buscar}
	 * filtra por código de retiro; {@code bandeja} acota por estado del ciclo (G9); {@code hoy} es la fecha
	 * del servidor para separar activas de vencidas.
	 */
	Pagina<Renta> listar(UUID empresaId, UUID clienteId, String buscar, FiltroDeBandeja bandeja, LocalDate hoy,
			SolicitudDePagina solicitud);

	/** Conteo de rentas por bandeja (números de las pestañas de G9). */
	ResumenDeRentas resumen(UUID empresaId, LocalDate hoy);

	/** Cuántas <b>unidades</b> de la prenda están comprometidas por rentas vigentes que se traslapan con el periodo (RF-3.2). */
	long cantidadSolapada(UUID empresaId, UUID prendaId, LocalDate retiro, LocalDate devolucion);

	/**
	 * Serializa las reservas de una misma prenda dentro de la transacción (evita la doble asignación
	 * por concurrencia, RF-0.4): dos reservas simultáneas de la misma prenda no corren a la vez.
	 */
	void bloquearReservaDePrenda(UUID prendaId);

	/** Renta con esa clave de idempotencia en la empresa, si existe (RF-17.6). */
	Optional<Renta> buscarPorClave(UUID empresaId, String claveIdempotencia);
}
