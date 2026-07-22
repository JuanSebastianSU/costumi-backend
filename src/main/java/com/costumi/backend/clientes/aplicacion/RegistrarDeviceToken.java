package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.dominio.Cliente;

import java.util.UUID;

/** Puerto de entrada: registrar/actualizar el token de dispositivo del cliente para push FCM (RF-18.11). */
public interface RegistrarDeviceToken {

	Cliente ejecutar(UUID empresaId, UUID clienteId, String deviceToken);

	/**
	 * El propio usuario registra el token de SU dispositivo. Lo guarda en todas sus fichas: un cliente
	 * tiene una ficha por tienda, y el telefono es el mismo para todas — si solo se guardara en una, las
	 * demas tiendas no podrian avisarle.
	 *
	 * @return cuantas fichas quedaron con el token.
	 */
	int deUsuario(UUID usuarioId, String deviceToken);
}
