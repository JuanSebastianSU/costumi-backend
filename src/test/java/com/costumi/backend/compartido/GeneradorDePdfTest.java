package com.costumi.backend.compartido;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** El generador de PDF produce documentos válidos (sin Spring ni BD). */
class GeneradorDePdfTest {

	private final GeneradorDePdf generador = new GeneradorDePdf();

	private static void esPdfValido(byte[] pdf) {
		assertThat(pdf).isNotEmpty();
		assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).startsWith("%PDF");
	}

	@Test
	void tabla_produce_un_pdf_valido() {
		byte[] pdf = generador.tabla("Reporte", new String[] { "Col A", "Col B" },
				List.of(new String[] { "1", "2" }, new String[] { "3", "4" }));
		esPdfValido(pdf);
	}

	@Test
	void documento_produce_un_pdf_valido() {
		byte[] pdf = generador.documento("Comprobante", List.of("Total: $100", "Estado: PAGADO"));
		esPdfValido(pdf);
	}

	@Test
	void tabla_sin_filas_igual_es_pdf_valido() {
		byte[] pdf = generador.tabla("Vacío", new String[] { "X" }, List.of());
		esPdfValido(pdf);
	}
}
