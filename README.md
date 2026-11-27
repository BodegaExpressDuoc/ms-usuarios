# ms-usuarios

Microservicio de gestión de usuarios del sistema BodegaExpress. Expone una API
REST para crear, consultar, actualizar y eliminar usuarios, con persistencia en
PostgreSQL.

## Tecnologías

- Java 17
- Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA, Jakarta Validation)
- PostgreSQL
- Lombok
- Maven (mediante Maven Wrapper)

## Requisitos previos

| Herramienta | Versión | Notas |
|---|---|---|
| JDK | 17 o superior | La variable `JAVA_HOME` debe apuntar al JDK |
| PostgreSQL | 14 o superior | Servicio en ejecución y accesible |
| Git | Cualquiera reciente | Para clonar el repositorio |
| Postman | Opcional | Para probar la API manualmente |

No es necesario instalar Maven: el repositorio incluye Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Instalación y ejecución local

Los comandos se muestran para Windows (PowerShell). En Linux o macOS, reemplazar
`.\mvnw.cmd` por `./mvnw` y `copy` por `cp`.

### 1. Clonar el repositorio

```powershell
git clone https://github.com/BodegaExpressDuoc/ms-usuarios.git
cd ms-usuarios
```

### 2. Crear la base de datos

Desde pgAdmin o psql, con un usuario con permisos de creación:

```sql
CREATE DATABASE bodega_usuarios;
```

Las tablas no se crean manualmente: Hibernate las crea o actualiza al iniciar la
aplicación (`spring.jpa.hibernate.ddl-auto: update`).

### 3. Configurar las variables de entorno

Las credenciales no se escriben en `application.yaml`; se leen desde variables de
entorno. Para desarrollo local se usa un archivo `.env`, excluido de Git:

```powershell
copy .env.example .env
```

Editar `.env` y completar `USUARIOS_DB_USERNAME`, `USUARIOS_DB_PASSWORD` y `JWT_SECRET`:

| Variable | Obligatoria | Valor por defecto | Descripción |
|---|---|---|---|
| `USUARIOS_DB_URL` | No | `jdbc:postgresql://localhost:5432/bodega_usuarios` | URL JDBC de la base de datos |
| `USUARIOS_DB_USERNAME` | **Sí** | — | Usuario de PostgreSQL |
| `USUARIOS_DB_PASSWORD` | **Sí** | — | Contraseña de PostgreSQL |
| `USUARIOS_ADDRESS` | No | `127.0.0.1` | Dirección de red en la que escucha el servicio |
| `USUARIOS_PORT` | No | `8081` | Puerto HTTP del servicio |
| `JWT_SECRET` | **Sí** | — | Clave para firmar los tokens JWT (texto aleatorio de al menos 32 caracteres) |
| `JWT_EXPIRACION_MINUTOS` | No | `60` | Minutos de validez del token |
| `GATEWAY_SECRET` | No | vacío | Secreto que el AWS API Gateway envía en el header `X-Gateway-Secret`. Si está definido, se rechazan con 403 las peticiones que no lo traen (solo se entra por el Gateway). Vacío = no se exige |

El archivo se escribe con el formato `NOMBRE=valor`, sin comillas. Spring Boot lo
carga mediante `spring.config.import`, sin dependencias adicionales, y debe
ubicarse en la carpeta desde donde se ejecuta la aplicación (la raíz del
proyecto). Las variables definidas en el sistema operativo tienen prioridad
sobre el archivo.

### 4. Compilar, probar y empaquetar

```powershell
.\mvnw.cmd clean install
```

Ejecuta las pruebas y genera el archivo `target/ms-usuarios.jar`.
Las pruebas de controlador usan un repositorio simulado, por lo que no requieren
PostgreSQL ni el archivo `.env`.

### 5. Ejecutar

Con Maven:

```powershell
.\mvnw.cmd spring-boot:run
```

O con el `.jar` generado:

```powershell
java -jar target/ms-usuarios.jar
```

El servicio queda disponible en `http://localhost:8081/api/v1/usuarios`.
Para detenerlo, usar `Ctrl+C`.

## Estructura del proyecto

```text
src/main/java/cl/duoc/bodegaexpress/usuarios/
├── MsUsuariosApplication.java   Punto de entrada de la aplicación
├── controller/                  Endpoints REST
├── service/                     Lógica de negocio y transacciones
├── repository/                  Acceso a datos con Spring Data JPA
└── model/                       Entidades JPA
```

## API

URL base: `http://localhost:8081/api/v1/usuarios`

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/v1/usuarios` | Lista todos los usuarios | `200` |
| GET | `/api/v1/usuarios/{id}` | Obtiene un usuario | `200`; `404` si no existe |
| POST | `/api/v1/auth/login` | Inicia sesión y entrega un token JWT | `200`; `401` si las credenciales son inválidas |
| POST | `/api/v1/usuarios` | Crea un usuario | `201` con cabecera `Location`; `409` si el email ya existe |
| PUT | `/api/v1/usuarios/{id}` | Reemplaza los datos de un usuario | `200`; `404` si no existe; `409` si el email ya existe |
| DELETE | `/api/v1/usuarios/{id}` | Elimina un usuario | `204`; `404` si no existe |

POST y PUT requieren `Content-Type: application/json`:

```json
{
  "nombre": "Carlos",
  "apellido": "Perez",
  "email": "carlos.perez@example.com",
  "password": "Clave123"
}
```

- Todos los campos son obligatorios y el email debe tener un formato válido.
- La contraseña se guarda encriptada (BCrypt) y nunca se incluye en las respuestas.
- El email es único; un email ya registrado devuelve `409 Conflict`.
- El `id` lo genera la base de datos.
- Los datos inválidos devuelven `400 Bad Request`.

### Autenticación

Las rutas están protegidas con JWT, excepto el registro (`POST /api/v1/usuarios`)
y el inicio de sesión (`POST /api/v1/auth/login`).

1. Iniciar sesión con el email y la contraseña de un usuario registrado:

   ```json
   POST /api/v1/auth/login
   {
     "email": "carlos.perez@example.com",
     "password": "Clave123"
   }
   ```

   Respuesta `200`:

   ```json
   { "token": "eyJhbGciOiJIUzI1NiJ9..." }
   ```

2. Enviar el token en las demás peticiones, en la cabecera:

   ```text
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
   ```

Sin token, o con un token inválido o vencido, el servicio responde `401 Unauthorized`.

### Formato de errores

Los errores se devuelven en formato
[ProblemDetail (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457), con el
código HTTP y un mensaje descriptivo. En los errores de validación se indica cada campo rechazado:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Los datos enviados no son válidos",
  "instance": "/api/v1/usuarios",
  "errores": {
    "nombre": "El nombre es obligatorio"
  }
}
```

## Pruebas manuales

1. Iniciar el servicio (paso 5).
2. Enviar un POST a `http://localhost:8081/api/v1/usuarios` con el JSON de ejemplo
   (en Postman: **Body → raw → JSON**) y guardar el `id` de la respuesta.
3. Iniciar sesión con POST `/api/v1/auth/login` y usar el token recibido en las
   siguientes peticiones (en Postman: **Authorization → Bearer Token**).
4. Consultar con GET `/api/v1/usuarios` y GET `/api/v1/usuarios/{id}`.
5. Modificar con PUT `/api/v1/usuarios/{id}`.
6. Verificar los datos en PostgreSQL:

   ```sql
   SELECT id, nombre, apellido, email FROM public.usuarios ORDER BY id;
   ```

7. Eliminar con DELETE `/api/v1/usuarios/{id}`; un GET posterior devuelve `404`.
8. Enviar un POST con el nombre vacío o un email inválido para obtener `400`.

## Pruebas con Postman

El repositorio incluye la colección
[`postman/ms-usuarios.postman_collection.json`](postman/ms-usuarios.postman_collection.json).

1. En Postman, seleccionar **File → Import** y elegir el archivo de la colección.
2. Iniciar el servicio (paso 5).
3. Abrir la colección, seleccionar **Run** y ejecutar las peticiones en orden.

Cada petición incluye pruebas automáticas que verifican el código HTTP y el
contenido de la respuesta. La variable `baseUrl` de la colección define la URL
del servicio. La colección crea un usuario de prueba, inicia sesión para obtener el
token (se guarda en la variable `token`) y elimina el usuario al final.

## Servicios relacionados

| Servicio | Puerto | Ruta base |
|---|---|---|
| ms-usuarios | 8081 | `/api/v1/usuarios` |
| ms-articulos | 8082 | `/api/v1/articulos` |
| ms-busqueda | 8083 | `/api/v1/busqueda` |
