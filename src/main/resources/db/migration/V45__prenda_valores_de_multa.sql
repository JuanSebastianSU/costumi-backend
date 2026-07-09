-- RF-5.2/5.6: la Prenda gana sus valores de multa, base para SUGERIR el cobro en la devolución cuando
-- una pieza se pierde (reposición) o vuelve dañada. Ambos opcionales; el cobro final lo decide el dueño.
alter table prenda add column valor_reposicion numeric(12, 2);
alter table prenda add column valor_dano numeric(12, 2);
