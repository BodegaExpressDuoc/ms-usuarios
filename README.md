# ms-usuarios

Microservicio de BodegaExpress para crear, consultar, actualizar y eliminar
usuarios, con persistencia en PostgreSQL.

## Tecnologías y requisitos

- Java 17 y Spring Boot 4.1.1.
- Spring Web MVC, Spring Data JPA y Jakarta Validation.
- Lombok para generar constructores, getters y setters.
- PostgreSQL disponible en `localhost:5432`.
- Maven Wrapper incluido; no es necesario instalar Maven por separado.

## Configuración local

La configuración se encuentra en [application.yaml](src/main/resources/application.yaml).

| Parámetro | Valor actual |
|---|---|
| Puerto HTTP | `8081` |
| Base de datos | `bodega_usuarios` |
| Usuario PostgreSQL | `postgres` |
| Contraseña local PostgreSQL | `postgres` |
| Tabla | `public.usuarios` |

Si preparas un entorno nuevo, crea la base desde pgAdmin o psql:

```sql
CREATE DATABASE bodega_usuarios;
```

En el entorno actual la base ya existe. Hibernate crea o actualiza la tabla al
arrancar mediante `spring.jpa.hibernate.ddl-auto: update`.

## Ejecutar

Desde una terminal PowerShell ubicada en `ms-usuarios`:

```powershell
.\mvnw.cmd spring-boot:run
```

Mantén la terminal abierta mientras pruebas la API. Para detenerla, usa `Ctrl+C`.

URL base: `http://localhost:8081/api/usuarios`.

## Estructura

```text
src/main/java/cl/duoc/bodegaexpress/usuarios/
├── MsUsuariosApplication.java
├── model/Usuario.java
├── repository/UsuarioRepository.java
├── service/UsuarioService.java
└── controller/UsuarioController.java
```

El controlador recibe las solicitudes HTTP y valida los datos. El servicio
ejecuta las operaciones dentro de transacciones y verifica que el usuario exista
al buscar, actualizar o eliminar. El repositorio usa JPA para acceder a PostgreSQL.

## API

| Método | Ruta | Resultado esperado |
|---|---|---|
| GET | `/api/usuarios` | `200`, lista; `[]` si no hay usuarios |
| GET | `/api/usuarios/{id}` | `200`, usuario; `404` si no existe |
| POST | `/api/usuarios` | `201`, usuario creado y cabecera `Location` |
| PUT | `/api/usuarios/{id}` | `200`, usuario actualizado; `404` si no existe |
| DELETE | `/api/usuarios/{id}` | `204`, sin cuerpo; `404` si no existe |

POST y PUT requieren `Content-Type: application/json` y los tres campos:

```json
{
  "nombre": "Carlos",
  "apellido": "Perez",
  "email": "carlos.prueba@example.com"
}
```

El identificador se genera al crear. PUT conserva el identificador de la ruta y
reemplaza nombre, apellido y email; no es una actualización parcial.

### Validaciones

- Nombre, apellido y email son obligatorios y no pueden contener solo espacios.
- El email debe tener un formato válido.
- Los errores de validación de POST y PUT devuelven `400 Bad Request`.
- El email tiene una restricción de unicidad en PostgreSQL. Actualmente no hay
  un manejo específico del correo duplicado: puede devolver `500`, no `409`.

## Prueba manual con Postman

1. Envía un POST a `http://localhost:8081/api/usuarios` con el JSON anterior,
   seleccionando **Body → raw → JSON**. Usa un correo que no exista.
2. Guarda el `id` recibido en la respuesta `201 Created`.
3. Envía GET a `/api/usuarios` y a `/api/usuarios/{id}` para consultar el registro.
4. Envía PUT a `/api/usuarios/{id}` con los tres campos, modificando el apellido.
5. Comprueba el cambio en PostgreSQL:

   ```sql
   SELECT id, nombre, apellido, email
   FROM public.usuarios
   ORDER BY id;
   ```

6. Si deseas eliminar el registro de prueba, envía DELETE a `/api/usuarios/{id}`.
   Un GET posterior de ese identificador debe devolver `404`.
7. Envía un POST con nombre vacío o email inválido para comprobar el `400`.

Estas operaciones escriben en la base local. Puedes conservar el usuario para
revisarlo en pgAdmin omitiendo el paso de eliminación.

## Prueba automática

```powershell
.\mvnw.cmd clean test
```

La prueba existente, `MsUsuariosApplicationTests.contextLoads`, comprueba que el
contexto de Spring Boot arranque. Requiere la base PostgreSQL y las credenciales
configuradas. No verifica individualmente las rutas HTTP ni las operaciones CRUD.
Este comando termina al completar las pruebas y no deja la API funcionando.

## Alcance actual

Esta versión implementa el CRUD de usuarios. La autenticación JWT y los roles
del diagrama de arquitectura están pendientes.

| Servicio local | Puerto | Ruta |
|---|---|---|
| ms-usuarios | 8081 | `/api/usuarios` |
| ms-articulos | 8082 | `/api/articulos` |
| ms-busqueda | 8083 | `/api/busqueda?nombre=caja` |
