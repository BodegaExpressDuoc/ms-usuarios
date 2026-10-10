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

Editar `.env` y completar al menos `USUARIOS_DB_PASSWORD`:

| Variable | Obligatoria | Valor por defecto | Descripción |
|---|---|---|---|
| `USUARIOS_DB_URL` | No | `jdbc:postgresql://localhost:5432/bodega_usuarios` | URL JDBC de la base de datos |
| `USUARIOS_DB_USERNAME` | No | `postgres` | Usuario de PostgreSQL |
| `USUARIOS_DB_PASSWORD` | **Sí** | — | Contraseña de PostgreSQL |
| `USUARIOS_PORT` | No | `8081` | Puerto HTTP del servicio |

El archivo se escribe con el formato `NOMBRE=valor`, sin comillas. Spring Boot lo
carga mediante `spring.config.import`, sin dependencias adicionales, y debe
ubicarse en la carpeta desde donde se ejecuta la aplicación (la raíz del
proyecto). Las variables definidas en el sistema operativo tienen prioridad
sobre el archivo.

### 4. Compilar, probar y empaquetar

```powershell
.\mvnw.cmd clean install
```

Ejecuta las pruebas y genera el archivo `target/ms-usuarios-0.0.1-SNAPSHOT.jar`.
Las pruebas inician el contexto de Spring Boot, por lo que requieren la base de
datos y el archivo `.env` configurados.

### 5. Ejecutar

Con Maven:

```powershell
.\mvnw.cmd spring-boot:run
```

O con el `.jar` generado:

```powershell
java -jar target/ms-usuarios-0.0.1-SNAPSHOT.jar
```

El servicio queda disponible en `http://localhost:8081/api/usuarios`.
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

URL base: `http://localhost:8081/api/usuarios`

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/usuarios` | Lista todos los usuarios | `200` |
| GET | `/api/usuarios/{id}` | Obtiene un usuario | `200`; `404` si no existe |
| POST | `/api/usuarios` | Crea un usuario | `201` con cabecera `Location` |
| PUT | `/api/usuarios/{id}` | Reemplaza los datos de un usuario | `200`; `404` si no existe |
| DELETE | `/api/usuarios/{id}` | Elimina un usuario | `204`; `404` si no existe |

POST y PUT requieren `Content-Type: application/json`:

```json
{
  "nombre": "Carlos",
  "apellido": "Perez",
  "email": "carlos.perez@example.com"
}
```

- Los tres campos son obligatorios y el email debe tener un formato válido.
- El email es único.
- El `id` lo genera la base de datos.
- Los datos inválidos devuelven `400 Bad Request`.

## Pruebas manuales

1. Iniciar el servicio (paso 5).
2. Enviar un POST a `http://localhost:8081/api/usuarios` con el JSON de ejemplo
   (en Postman: **Body → raw → JSON**) y guardar el `id` de la respuesta.
3. Consultar con GET `/api/usuarios` y GET `/api/usuarios/{id}`.
4. Modificar con PUT `/api/usuarios/{id}`.
5. Verificar los datos en PostgreSQL:

   ```sql
   SELECT id, nombre, apellido, email FROM public.usuarios ORDER BY id;
   ```

6. Eliminar con DELETE `/api/usuarios/{id}`; un GET posterior devuelve `404`.
7. Enviar un POST con el nombre vacío o un email inválido para obtener `400`.

## Servicios relacionados

| Servicio | Puerto | Ruta base |
|---|---|---|
| ms-usuarios | 8081 | `/api/usuarios` |
| ms-articulos | 8082 | `/api/articulos` |
| ms-busqueda | 8083 | `/api/busqueda` |
