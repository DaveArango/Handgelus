# Handgelus

Sistema web para el atelier **Handgelus** (tatuaje de autor y arte por comisión, Armenia – Quindío).
Centraliza solicitudes, calcula cotizaciones, controla abonos y anticipos, gestiona la agenda del
tatuador y permite seguir el avance de cada obra.

| Capa | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot 3.5 · Spring Security (JWT + bcrypt) · Spring Data JPA |
| Base de datos | **Oracle** (XE 21c o 23ai Free). Perfil opcional `h2` para probar sin Oracle |
| Construcción | Gradle (wrapper incluido) · configuración en `application.properties` |
| Frontend | Angular 19 (standalone components, signals) |
| IDE | IntelliJ IDEA |

---

## 1. Arquitectura hexagonal

```
                 ┌──────────────── infrastructure ────────────────┐
                 │  adapter/in/web        (REST controllers)       │
   Angular  ───▶ │  adapter/in/programador (tareas @Scheduled)     │
                 │                                                 │
                 │      ┌──────────── application ────────────┐    │
                 │      │  port/in   (casos de uso F-01..F-08) │    │
                 │      │  service   (implementación)          │    │
                 │      │      ┌────── domain ──────┐          │    │
                 │      │      │ modelos y reglas   │          │    │
                 │      │      │ RN-01 … RN-12      │          │    │
                 │      │      └────────────────────┘          │    │
                 │      │  port/out  (repositorios, pasarela,  │    │
                 │      │             correo, archivos, JWT)   │    │
                 │      └──────────────────────────────────────┘    │
                 │  adapter/out/persistence (JPA · Oracle)          │
                 │  adapter/out/pasarela    (pasarela simulada)     │
                 │  adapter/out/correo      (correo simulado)       │
                 │  adapter/out/almacenamiento, seguridad           │
                 └──────────────────────────────────────────────────┘
```

- `domain` y `application` son **Java puro**: no tienen anotaciones de Spring ni de JPA.
- `infrastructure/config/BeanConfig.java` conecta los puertos con los adaptadores y aplica las
  transacciones a los casos de uso por AOP, sin ensuciar la capa de aplicación.
- La prueba `FlujoNegocioTest` ejecuta las 8 funcionalidades con adaptadores en memoria: demuestra
  que el núcleo funciona sin Spring ni Oracle.

```
handgelus/
├── backend/                       Spring Boot (subproyecto Gradle :backend)
│   └── src/main/java/com/handgelus/
│       ├── domain/                model · service · exception
│       ├── application/           port/in · port/out · service
│       └── infrastructure/        adapter/in · adapter/out · config
├── frontend/                      Angular 19
│   └── src/app/                   core · shared · layout · pages/{publico,cliente,estudio}
├── docker-compose.yml             Oracle 23ai Free (opcional)
├── .run/                          Configuraciones de ejecución para IntelliJ
└── build.gradle · settings.gradle · gradlew
```

---

## 2. Las 8 funcionalidades (plan de requisitos)

| ID | Funcionalidad | Dónde está en la app | Reglas implementadas |
|---|---|---|---|
| F-01 | Gestionar usuarios | Atelier → **Usuarios** (solo administrador) | SWR-01 rol único · SWR-02 acceso por rol · SWR-03 bcrypt + HTTPS configurable |
| F-02 | Configurar tabla de tarifas | Atelier → **Tarifas** (tatuador/propietario y administrador) | SWR-04 · SWR-05 la cotización guarda copia de la tarifa |
| F-03 | Registrar solicitud | Cliente → **Nueva solicitud** | SWR-06 correo único · SWR-07 imagen ≤ 5 MB · SWR-08 dirección completa |
| F-04 | Generar cotización | Cálculo automático + Atelier → ficha de la solicitud | RN-03 tarifa fija · RN-04 sesiones · RN-09 envío · solo el tatuador confirma · correo al cliente · aceptar/rechazar |
| F-05 | Registrar abono o anticipo | Cliente → pasarela · Atelier → pago manual · **Control de pagos** | 30 % / 50 % mínimo · webhook firmado · reversión a las 48 h |
| F-06 | Agendar cita de tatuaje | Atelier → **Agenda** y ficha de la solicitud | RN-01 sin abono no hay cita · 2 citas/día · jornada ocupada sugiere fechas · comprobante con fecha, hora y valor |
| F-07 | Actualizar avance de obra | Atelier → **Proyectos en curso** y ficha · Cliente → su proyecto | En espera → producción → terminada → enviada · SWR-19 el cliente solo ve lo suyo · SWR-20 no se envía con saldo |
| F-08 | Consultar historial | Atelier → **Clientes** · Cliente → **Perfil y pagos** | SWR-21 tipo, fecha, valor, estado y comprobantes |

Extras del contexto del negocio: tablero con indicadores, bandeja de notificaciones (alertas internas
y copia de cada correo enviado), alerta si una cotización lleva 72 h sin confirmar (CU-02 4.a),
cierre del servicio solo con pago completo (RN-07), comprobantes imprimibles.

---

## 3. Cómo ejecutarlo

### Requisitos
- JDK 21
- Node.js 20 o superior (incluye npm)
- Oracle XE 21c / 23ai Free **o** Docker **o** nada (perfil `h2`)

### 3.1 Base de datos Oracle (elige una opción)

**A. Oracle instalado localmente** — ejecuta como SYSTEM el script
`backend/src/main/resources/db/oracle-crear-usuario.sql` sobre la PDB `XEPDB1` (XE 21c).
La conexión por defecto es `jdbc:oracle:thin:@localhost:1521/XEPDB1`, usuario `HANDGELUS`,
contraseña `handgelus` (ver `backend/src/main/resources/application.properties`).

**B. Docker** — `docker compose up -d` crea Oracle 23ai Free con el usuario listo. Arranca el
backend con la variable de entorno `DB_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1`.

**C. Sin Oracle** — usa el perfil `h2` (base embebida en modo compatible con Oracle).

Las tablas (`HG_USUARIO`, `HG_SOLICITUD`, `HG_COTIZACION`, `HG_PAGO`, `HG_CITA`, `HG_OBRA`,
`HG_AVANCE`, `HG_COMPROBANTE`, `HG_NOTIFICACION`, `HG_TARIFA`) las crea Hibernate al arrancar.

### 3.2 En IntelliJ IDEA
1. **File → Open** y selecciona la carpeta `handgelus` (la raíz). IntelliJ detecta Gradle y descarga las dependencias.
2. Verifica **Settings → Build Tools → Gradle → Gradle JVM = 21**.
3. En el selector de ejecución aparecen tres configuraciones (carpeta `.run/`):
   - **Backend - Oracle** → `:backend:bootRun`
   - **Backend - H2 (sin Oracle)** → `:backend:bootRun --args='--spring.profiles.active=h2'`
   - **Frontend - Angular** → `npm start` (ejecuta antes `npm install` dentro de `frontend/`)
4. Abre **http://localhost:4200**. El frontend redirige `/api` al backend en el puerto 8080 (`frontend/proxy.conf.json`).

### 3.3 Por consola
```bash
./gradlew :backend:bootRun                                      # Oracle
./gradlew :backend:bootRun --args='--spring.profiles.active=h2' # H2
cd frontend && npm install && npm start
./gradlew :backend:test                                         # pruebas
```

### 3.4 Cuentas de demostración
La primera vez se cargan datos de ejemplo usando los mismos casos de uso (solicitudes en distintas
etapas, pagos, citas y una obra en producción).

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@handgelus.co | Admin2026 |
| Asistente | asistente@handgelus.co | Asistente2026 |
| Tatuador / Artista | artista@handgelus.co | Artista2026 |
| Cliente | valeria@correo.com | Cliente2026 |

Para no cargarlos: `handgelus.datos-iniciales.habilitado=false`.

---

## 4. Integraciones simuladas

- **Pasarela de pago** (`PasarelaPagoSimuladaAdapter`): al pagar, el cliente ve una pantalla de
  pasarela donde aprueba o rechaza; esta dispara el webhook firmado `POST /api/pagos/webhook`
  (cabecera `X-Firma-Pasarela`). Para MercadoPago/PayU basta otra implementación de `PasarelaPagoPort`.
- **Correo** (`CorreoSimuladoAdapter`): cada correo se escribe en el log y queda en Atelier →
  **Notificaciones → Correos a clientes**. Para correo real, otra implementación de `EnvioCorreoPort`.
- **HTTPS** (SWR-03): genera un keystore con el comando que aparece en `application.properties`,
  descomenta `server.ssl.*` y activa `handgelus.seguridad.requerir-https=true`.

Las reglas parametrizables (30 % de abono, 50 % de anticipo, 48 h, 72 h, 2 citas por día, 5 MB,
costos de envío) están en `application.properties` bajo `handgelus.negocio.*`.

---

## 5. API REST principal

| Método | Ruta | Uso |
|---|---|---|
| POST | `/api/auth/login` · `/api/auth/registro` | Ingreso y registro de clientes |
| GET/POST/PUT/PATCH | `/api/usuarios` | F-01 (administrador) |
| GET/PUT | `/api/tarifas` | F-02 |
| GET | `/api/cotizaciones/estimar` | Valor de referencia (RF-04) |
| POST/GET | `/api/solicitudes` (multipart) · `/api/solicitudes/{id}` | F-03 |
| POST | `/api/solicitudes/{id}/cotizacion/confirmar` · `/decision` | F-04 |
| POST | `/api/solicitudes/{id}/pagos` · `/pagos/manual` · `/api/pagos/webhook` | F-05 |
| GET/POST | `/api/agenda/disponibilidad` · `/sugerencias` · `/citas` | F-06 |
| POST | `/api/solicitudes/{id}/obra/estado` · `/avances` · GET `/api/proyectos` | F-07 |
| GET | `/api/clientes` · `/api/clientes/{id}/historial` · `/api/mi-historial` | F-08 |
| GET | `/api/tablero` · `/api/notificaciones` · `/api/catalogos` | Panel |

---

## 6. Identidad visual

Paleta de papel de boceto y tinta sepia: lino `#EFE7DA`, pergamino `#F8F3EB`, tinta `#2A211C`,
sepia `#6B3F26`, latón `#A88A55`. Tipografías Cormorant Garamond (titulares) y Mulish (texto).
La portada muestra una ilustración botánica de línea fina (helecho, orquídea y café del Quindío)
que se dibuja al cargar, como el stencil que el artista calca antes de tatuar.
