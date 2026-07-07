package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.dominio.Cliente;

import java.util.UUID;

/** Puerto de entrada: registrar/actualizar el token de dispositivo del cliente para push FCM (RF-18.11). */
public interface RegistrarDeviceToken {

	Cliente ejecutar(UUID empresaId, UUID clienteId, String deviceToken);
}
