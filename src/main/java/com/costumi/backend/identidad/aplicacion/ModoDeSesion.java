package com.costumi.backend.identidad.aplicacion;

/**
 * El contexto en el que opera una sesión (Fase B, H1). Una persona es siempre cliente; si además tiene una
 * membresía de trabajo activa, puede alternar entre comprar y trabajar:
 * <ul>
 *   <li>{@code COMPRA}: token de cliente (sin empresa) — explora tiendas y hace pedidos.</li>
 *   <li>{@code TRABAJO}: token con la empresa+rol de su membresía activa — opera en su tienda.</li>
 * </ul>
 */
public enum ModoDeSesion {
	COMPRA,
	TRABAJO
}
