-- RF-5.5: una pieza faltante/perdida no cierra la renta hasta devolverla o marcarla perdida + cobrada.
-- perdida_cobrada indica que la pérdida de esa pieza ya se cobró (reposición) y por tanto queda resuelta.
alter table pieza_revisada add column perdida_cobrada boolean not null default false;
