-- =====================================================================
--  HANDGELUS · Creación del esquema en Oracle
--  Ejecutar conectado como SYSTEM (o SYS as SYSDBA) sobre la PDB:
--    Oracle XE 21c   -> XEPDB1
--    Oracle 23ai Free -> FREEPDB1
--  Ejemplo con SQL*Plus:
--    sqlplus system/<clave>@localhost:1521/XEPDB1 @oracle-crear-usuario.sql
--
--  Las tablas (prefijo HG_) las crea Hibernate al iniciar la aplicación
--  (spring.jpa.hibernate.ddl-auto=update).
-- =====================================================================

CREATE USER HANDGELUS IDENTIFIED BY handgelus
  DEFAULT TABLESPACE USERS
  TEMPORARY TABLESPACE TEMP
  QUOTA UNLIMITED ON USERS;

GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW, CREATE TRIGGER TO HANDGELUS;

-- Para reiniciar el esquema completamente:
-- DROP USER HANDGELUS CASCADE;
