package com.costumi.backend.inventario.adaptadores.salida;

import com.costumi.backend.inventario.aplicacion.AlmacenDeImagenesNoConfigurado;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El almacén S3 está gateado: sin bucket/región no está disponible y subir da un error claro (503). */
class AlmacenDeImagenesS3Test {

	@Test
	void sin_configuracion_no_esta_disponible_y_subir_lanza_no_configurado() {
		AlmacenDeImagenesS3 s3 = new AlmacenDeImagenesS3("", "");
		assertThat(s3.disponible()).isFalse();
		assertThatThrownBy(() -> s3.subir(new byte[] { 1, 2, 3 }, "image/png", "clave"))
				.isInstanceOf(AlmacenDeImagenesNoConfigurado.class);
	}

	@Test
	void con_bucket_y_region_esta_disponible() {
		AlmacenDeImagenesS3 s3 = new AlmacenDeImagenesS3("mi-bucket", "us-east-1");
		assertThat(s3.disponible()).isTrue();
	}
}
