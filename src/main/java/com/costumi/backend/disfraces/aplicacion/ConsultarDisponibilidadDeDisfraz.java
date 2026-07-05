package com.costumi.backend.disfraces.aplicacion;

import java.util.UUID;

/** Puerto de entrada: disponibilidad derivada de un disfraz (RF-2.4), acotada al tenant. */
public interface ConsultarDisponibilidadDeDisfraz {

	boolean estaDisponible(UUID empresaId, UUID disfrazId);
}
