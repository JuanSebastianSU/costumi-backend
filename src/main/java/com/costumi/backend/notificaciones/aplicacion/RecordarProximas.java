package com.costumi.backend.notificaciones.aplicacion;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: recordatorio anticipado a los clientes cuya renta vence pronto (RF-11.1). */
public interface RecordarProximas {

	/** Envía el recordatorio a las rentas de la empresa que vencen dentro de la ventana y devuelve cuántos. */
	int ejecutar(UUID empresaId);

	/** Empresas con al menos una renta que vence dentro de la ventana: las que el job debe recordar. */
	List<UUID> empresasConProximas();
}
