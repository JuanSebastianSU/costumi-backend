package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.configuracion.ConsultaDeConfiguracion;
import com.costumi.backend.pagos.dominio.IntentoDePago;
import com.costumi.backend.pagos.dominio.IntentoDePagoRepository;
import com.costumi.backend.pagos.dominio.PasarelaDePago;
import com.costumi.backend.pagos.dominio.TipoConcepto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inicia un pago en línea (RF-6.11): exige que la empresa tenga el switch pagoEnLinea activo, crea el
 * checkout en la pasarela y persiste el intento (para que el webhook pueda confirmarlo luego).
 */
@Service
class CrearIntentoDePagoService implements CrearIntentoDePago {

	private final IntentoDePagoRepository intentos;
	private final PasarelaDePago pasarela;
	private final ConsultaDeConfiguracion configuracion;

	CrearIntentoDePagoService(IntentoDePagoRepository intentos, PasarelaDePago pasarela,
			ConsultaDeConfiguracion configuracion) {
		this.intentos = intentos;
		this.pasarela = pasarela;
		this.configuracion = configuracion;
	}

	@Override
	@Transactional
	public Resultado ejecutar(UUID empresaId, UUID sucursalId, UUID empleadoId, TipoConcepto tipoConcepto,
			UUID conceptoId, BigDecimal monto, String moneda) {
		if (!configuracion.pagoEnLinea(empresaId)) {
			throw new PagoEnLineaDeshabilitado();
		}
		IntentoDePago intento = IntentoDePago.crear(empresaId, sucursalId, empleadoId, tipoConcepto, conceptoId, monto,
				moneda);
		PasarelaDePago.ResultadoCheckout checkout = pasarela.crearCheckout(intento.monto(), intento.moneda(),
				intento.id().toString(), "Pago " + tipoConcepto + " " + conceptoId);
		intento.asignarReferenciaExterna(checkout.idExterno());
		intentos.guardar(intento);
		return new Resultado(intento.id(), checkout.urlCheckout());
	}
}
