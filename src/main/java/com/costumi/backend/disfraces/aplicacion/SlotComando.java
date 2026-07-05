package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.EjeDePrenda;
import com.costumi.backend.disfraces.dominio.EjeDeTalla;

import java.util.UUID;

/** Datos de un slot al crear un disfraz por partes. {@code pool} solo aplica si el eje de prenda es personalizable. */
public record SlotComando(int orden, String nombre, EjeDeTalla ejeTalla, String tallaFija, EjeDePrenda ejePrenda,
		UUID prendaFijaId, PoolComando pool, boolean opcional) {
}
