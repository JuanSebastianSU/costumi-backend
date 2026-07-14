package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.clientes.ResolucionDeClientes;
import com.costumi.backend.identidad.ConsultaDeSucursales;
import com.costumi.backend.notificaciones.dominio.AvisosDeProximasReadRepository;
import com.costumi.backend.notificaciones.dominio.CanalNotificacion;
import com.costumi.backend.notificaciones.dominio.PlantillaDeNotificacion;
import com.costumi.backend.notificaciones.dominio.RentaProximaAviso;
import com.costumi.backend.notificaciones.dominio.TipoDeEvento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Recordatorio anticipado (RF-11.1): a los clientes cuya renta vence dentro de {@code diasAntes} días se
 * les avisa con la plantilla configurable {@link TipoDeEvento#RECORDATORIO_DEVOLUCION}, resolviendo
 * cliente, fecha, días restantes y la ubicación de la sucursal. La ventana es configurable por propiedad.
 */
@Service
class RecordatorioProximasService implements RecordarProximas {

	private final AvisosDeProximasReadRepository proximas;
	private final EnviarNotificacion enviarNotificacion;
	private final PlantillaDeEvento plantillas;
	private final ResolucionDeClientes clientes;
	private final ConsultaDeSucursales sucursales;
	private final int diasAntes;

	RecordatorioProximasService(AvisosDeProximasReadRepository proximas, EnviarNotificacion enviarNotificacion,
			PlantillaDeEvento plantillas, ResolucionDeClientes clientes, ConsultaDeSucursales sucursales,
			@Value("${costumi.notificaciones.recordatorio-proximas.dias-antes:1}") int diasAntes) {
		this.proximas = proximas;
		this.enviarNotificacion = enviarNotificacion;
		this.plantillas = plantillas;
		this.clientes = clientes;
		this.sucursales = sucursales;
		this.diasAntes = diasAntes;
	}

	@Override
	@Transactional(readOnly = true)
	public List<UUID> empresasConProximas() {
		return proximas.empresasConProximas(LocalDate.now().plusDays(diasAntes));
	}

	@Override
	@Transactional
	public int ejecutar(UUID empresaId) {
		PlantillaDeNotificacion plantilla = plantillas.para(empresaId, TipoDeEvento.RECORDATORIO_DEVOLUCION);
		if (!plantilla.activa()) {
			return 0; // la empresa apagó el recordatorio anticipado.
		}
		LocalDate fechaObjetivo = LocalDate.now().plusDays(diasAntes);
		int enviadas = 0;
		for (RentaProximaAviso aviso : proximas.proximas(empresaId, fechaObjetivo)) {
			if (aviso.clienteId() == null) {
				continue;
			}
			Map<String, String> variables = new HashMap<>();
			variables.put("cliente", clientes.nombreDeCliente(empresaId, aviso.clienteId()).orElse("cliente"));
			variables.put("fecha_devolucion", String.valueOf(aviso.fechaDevolucion()));
			variables.put("dias_restantes", String.valueOf(diasAntes));
			VariablesDeSucursal.agregar(variables, sucursales, empresaId, aviso.sucursalId());
			enviarNotificacion.ejecutar(new EnviarNotificacionComando(empresaId, aviso.clienteId(),
					CanalNotificacion.WHATSAPP, plantilla.render(variables)));
			enviadas++;
		}
		return enviadas;
	}
}
