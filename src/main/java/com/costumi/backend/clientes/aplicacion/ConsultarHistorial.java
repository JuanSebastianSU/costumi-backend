package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.dominio.CargaDeCliente;
import com.costumi.backend.clientes.dominio.DeudaEnTienda;
import com.costumi.backend.clientes.dominio.FiltroDeClientes;
import com.costumi.backend.clientes.dominio.FiltroDeHistorial;
import com.costumi.backend.clientes.dominio.HistorialItem;
import com.costumi.backend.clientes.dominio.LineaDeEstadoDeCuenta;
import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Puerto de entrada: historial de un cliente y clientes con pendientes (RF-7.2/11.5). */
public interface ConsultarHistorial {

	List<HistorialItem> historialDeCliente(UUID empresaId, UUID clienteId);

	/** Estado de cuenta del cliente: desglose por renta de cuánto debe y por qué (RF-7/11.5). */
	List<LineaDeEstadoDeCuenta> estadoDeCuenta(UUID empresaId, UUID clienteId);

	/**
	 * "Mis Pedidos" del usuario del marketplace, paginado y filtrado por pestaña, uniendo sus fichas en
	 * todas las tiendas (RF-14.4/18.9).
	 */
	Pagina<HistorialItem> historialDeUsuario(UUID usuarioId, FiltroDeHistorial filtro, SolicitudDePagina solicitud);

	/** Detalle de una operación del propio usuario (por sus fichas), para "Mis Pedidos". Vacío si no es suya. */
	Optional<HistorialItem> operacionDeUsuario(UUID usuarioId, UUID operacionId);

	/** Multas y saldos del propio cliente, en todas las tiendas (RF-7/11.5). */
	List<DeudaEnTienda> misDeudas(UUID usuarioId);

	/** Ids de clientes de la empresa que caen en la categoría de pendiente indicada (RF-11.5/11.6). */
	List<UUID> clientesPorFiltro(UUID empresaId, FiltroDeClientes filtro);

	/** Saldo pendiente y multa por cliente (RF-7/11.5) para los clientes dados (la página actual). */
	Map<UUID, CargaDeCliente> cargaDeClientes(UUID empresaId, Collection<UUID> clienteIds);
}
