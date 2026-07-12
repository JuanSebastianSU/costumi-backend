package com.costumi.backend.identidad.aplicacion;

import java.time.Instant;

/** Un token de refresco recién emitido: su valor y el instante en que vence (C2). */
public record RefreshEmitido(String token, Instant expiraEn) {
}
