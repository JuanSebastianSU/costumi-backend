package com.costumi.backend.pagos.aplicacion;

/** Puerto de entrada: registrar un cobro mixto (varias porciones/métodos) con cálculo de vuelto (RF-6.7). */
public interface RegistrarCobroMixto {

	ResultadoCobroMixto ejecutar(RegistrarCobroMixtoComando comando);
}
