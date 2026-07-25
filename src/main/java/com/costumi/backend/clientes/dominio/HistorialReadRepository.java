package com.costumi.backend.clientes.dominio;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: historial de operaciones de un cliente (RF-7.2), acotado al tenant. */
public interface HistorialReadRepository {

	List<HistorialItem> deCliente(UUID empresaId, UUID clienteId);

	/**
	 * "Mis Pedidos" del usuario del marketplace, paginado y por recencia, cruzando sus fichas en todas las
	 * tiendas ({@code cliente.usuario_id}) en UNA consulta (antes era un N+1 por tienda + orden en memoria).
	 * {@code filtro} acota por pestaña (por pagar / por retirar / activos / cerrados).
	 */
	Pagina<HistorialItem> historialDeUsuario(UUID usuarioId, FiltroDeHistorial filtro, SolicitudDePagina solicitud);

	/**
	 * Una operación puntual del propio usuario (para el detalle de "Mis Pedidos"), resuelta por sus fichas
	 * para que solo pueda ver las suyas. Vacío si no existe o no es de él.
	 */
	Optional<HistorialItem> operacionDeUsuario(UUID usuarioId, UUID operacionId);

	/**
	 * Estado de cuenta del cliente (RF-7/11.5): una línea por renta con saldo o multa, con el desglose
	 * (importe, daños, retraso, depósito, multa, pagado, saldo) que explica cuánto debe y por qué.
	 */
	List<LineaDeEstadoDeCuenta> estadoDeCuenta(UUID empresaId, UUID clienteId);

	/**
	 * Lo que el USUARIO debe en todas las tiendas (RF-7/11.5 desde el lado del cliente): sus multas y
	 * saldos, con el nombre de cada tienda. Cruza sus fichas, igual que su historial.
	 */
	List<DeudaEnTienda> estadoDeCuentaDeUsuario(UUID usuarioId);

	/**
	 * Ids de clientes de la empresa que caen en la categoría de pendiente indicada (RF-11.5/11.6).
	 * {@code hoy} se usa para las rentas vencidas.
	 */
	List<UUID> clientesPorFiltro(UUID empresaId, FiltroDeClientes filtro, LocalDate hoy);

	/**
	 * Saldo pendiente y multa acumulada por cliente (RF-7/11.5), solo para los {@code clienteIds} dados
	 * (la página actual). Los clientes sin carga no aparecen en el mapa.
	 */
	Map<UUID, CargaDeCliente> cargaDeClientes(UUID empresaId, Collection<UUID> clienteIds);
}
