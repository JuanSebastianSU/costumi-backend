package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.Disfraz;

import java.util.UUID;

/** Puerto de entrada: archivar/activar un Disfraz (RF-2.3), acotado al tenant. */
public interface CambiarEstadoDisfraz {

	/** Lo retira de la vitrina y del alta de rentas sin borrarlo. */
	Disfraz archivar(UUID empresaId, UUID disfrazId);

	/** Lo vuelve a ofrecer en la vitrina. */
	Disfraz activar(UUID empresaId, UUID disfrazId);
}
