-- Token de dispositivo del cliente para notificaciones push por FCM (RF-18.11). Opcional.
-- (El teléfono para WhatsApp, RF-11.4, ya existe en la tabla cliente.)
alter table cliente add column device_token varchar(300);
