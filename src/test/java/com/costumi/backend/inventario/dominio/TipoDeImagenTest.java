package com.costumi.backend.inventario.dominio;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Detección de imagen por magic bytes (RF-2.9, C1): puro, sin BD ni Spring. */
class TipoDeImagenTest {

	@Test
	void detecta_png_jpeg_y_webp_por_su_firma() {
		byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
		byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2};
		byte[] webp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 1};

		assertThat(TipoDeImagen.detectar(png)).contains(TipoDeImagen.PNG);
		assertThat(TipoDeImagen.detectar(jpeg)).contains(TipoDeImagen.JPEG);
		assertThat(TipoDeImagen.detectar(webp)).contains(TipoDeImagen.WEBP);
	}

	@Test
	void rechaza_contenido_que_no_es_imagen() {
		assertThat(TipoDeImagen.detectar("no soy una imagen".getBytes())).isEmpty();
		assertThat(TipoDeImagen.detectar(new byte[] {1, 2, 3})).isEmpty();
		assertThat(TipoDeImagen.detectar(new byte[0])).isEmpty();
		assertThat(TipoDeImagen.detectar(null)).isEmpty();
	}

	@Test
	void el_tipo_da_una_extension_y_content_type_canonicos() {
		assertThat(TipoDeImagen.PNG.extension()).isEqualTo(".png");
		assertThat(TipoDeImagen.PNG.contentType()).isEqualTo("image/png");
		assertThat(TipoDeImagen.JPEG.extension()).isEqualTo(".jpg");
	}
}
