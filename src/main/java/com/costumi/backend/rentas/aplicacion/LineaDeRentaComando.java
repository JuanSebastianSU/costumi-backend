package com.costumi.backend.rentas.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** Un artículo de la renta a crear: prenda, cantidad y precio por día pactado. */
public record LineaDeRentaComando(UUID prendaId, int cantidad, BigDecimal precioPorDia) {
}
