# Plantilla / esqueleto de módulo (spec §5.3)

Todo módulo nuevo se crea con **la misma estructura de paquetes**. Esto lo verifican el
build (ArchUnit) y Spring Modulith; no es una recomendación, es una regla mecánica.

## Estructura de paquetes (idéntica en todos los módulos)

```
com.costumi.backend.<modulo>/
├── dominio/                 # Entidades, agregados, value objects, puertos (interfaces).
│                            # CERO framework: no importa Spring, JPA ni web.
├── aplicacion/              # Casos de uso / servicios de aplicación. Orquesta el dominio.
│                            # Depende solo de dominio (puertos).
└── adaptadores/
    ├── entrada/             # Controllers REST, listeners de eventos. Traduce mundo -> aplicacion.
    │                        # Aquí viven los DTOs de request/response (nunca entidades).
    └── salida/              # Repositorios JPA, clientes HTTP, publicadores. Implementa los puertos.
```

## Dirección de dependencias (la verifica ArchUnit)

`adaptadores.entrada` → `aplicacion` → `dominio` ← `adaptadores.salida`

Las dependencias **apuntan hacia adentro**. El dominio no conoce a nadie hacia afuera.

## Nivel de rigor por módulo (spec §5.2)

- **Hexagonal completo** (puertos/adaptadores): Identidad y tenant, Catálogo y taxonomía,
  Inventario y disponibilidad, Pedidos/carrito, Rentas, Ventas/POS, Pagos/caja/depósitos,
  Devoluciones y multas.
- **Simple** (capas ligeras: `aplicacion` + `adaptadores`, sin puerto elaborado): Clientes,
  Empleados, Reportes (lectura), Notificaciones (adaptador), Configuración de empresa.

Un módulo "simple" **sube** a hexagonal cuando acumula reglas de negocio propias; nunca al revés.

## Checklist al crear un módulo (Definition of Done, CLAUDE.md)

- [ ] Paquetes `dominio/aplicacion/adaptadores.{entrada,salida}` creados.
- [ ] Puertos definidos; sin fugas de framework al dominio.
- [ ] Tests de dominio pasan sin BD; ArchUnit y Modulith en verde.
- [ ] Migración Flyway incluida; toda tabla lleva `empresa_id` (y `sucursal_id` donde aplique).
- [ ] DTOs en la frontera; contrato OpenAPI actualizado si cambió la API.
- [ ] `PROGRESS.md` actualizado.
