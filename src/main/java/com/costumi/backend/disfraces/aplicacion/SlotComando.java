package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.EjeDePrenda;

import java.util.UUID;

/** Datos de un slot al crear/editar un disfraz. {@code pool} solo aplica si el eje de prenda es personalizable. */
public record SlotComando(int orden, String nombre, EjeDePrenda ejePrenda, UUID prendaFijaId, PoolComando pool,
		boolean opcional) {
}
