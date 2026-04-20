# Demo Spring WebFlux REST API

Servicio REST reactivo con **Spring WebFlux + R2DBC + H2** diseñado para cubrir todos los patrones de consumo HTTP. Ideal para probar librerías cliente HTTP.

---

## Requisitos
- Java 17+
- Maven 3.8+

## Ejecución
```bash
mvn spring-boot:run
# o
mvn clean package && java -jar target/demo-webflux-1.0.0.jar
```

Servidor en: `http://localhost:8080`  
H2 Console:  `http://localhost:8080/h2-console`  
→ JDBC URL: `jdbc:h2:mem:testdb`  

---

## Endpoints

### 👤 Users  `/api/users`

| Método | URL | Descripción | Patrón |
|--------|-----|-------------|--------|
| GET    | `/api/users` | Todos los usuarios | - |
| GET    | `/api/users?active=true` | Filtrar por activo | Query param booleano |
| GET    | `/api/users?page=0&size=3` | Paginación | Múltiples query params |
| GET    | `/api/users/search?name=juan` | Buscar por nombre | Query param string |
| GET    | `/api/users/age-range?min=20&max=40` | Rango de edad | Dos query params numéricos |
| GET    | `/api/users/{id}` | Usuario por ID | Path variable |
| GET    | `/api/users/{id}/with-headers` | Usuario + captura headers | Request headers custom |
| POST   | `/api/users` | Crear usuario | JSON body + validación |
| PUT    | `/api/users/{id}` | Actualización completa | Path var + JSON body |
| PATCH  | `/api/users/{id}` | Actualización parcial | Path var + JSON parcial |
| DELETE | `/api/users/{id}` | Eliminar usuario | Path variable |
| DELETE | `/api/users` | Eliminar todos | Header `X-Confirm-Delete: yes` |

**Headers especiales en GET `/api/users/{id}/with-headers`:**
```
X-Client-ID: mi-app
X-Request-Source: mobile
Accept-Language: es-MX
```

---

### 📦 Products  `/api/products`

| Método | URL | Descripción |
|--------|-----|-------------|
| GET    | `/api/products` | Todos los productos |
| GET    | `/api/products?page=0&size=5` | Paginación |
| GET    | `/api/products/search?name=laptop` | Búsqueda por nombre |
| GET    | `/api/products/category/{category}` | Por categoría (ELECTRONICS, FURNITURE, AUDIO) |
| GET    | `/api/products/price-range?min=50&max=500` | Rango de precio |
| GET    | `/api/products/low-stock?threshold=20` | Stock bajo |
| GET    | `/api/products/{id}` | Producto por ID |
| POST   | `/api/products` | Crear producto |
| PUT    | `/api/products/{id}` | Actualización completa |
| PATCH  | `/api/products/{id}` | Actualización parcial |
| PATCH  | `/api/products/{id}/stock?quantity=50` | Solo actualiza stock |
| DELETE | `/api/products/{id}` | Eliminar producto |

---

### 📁 Files  `/api/files`

| Método | URL | Descripción | Content-Type |
|--------|-----|-------------|--------------|
| POST   | `/api/files/upload` | Subir un archivo | `multipart/form-data` → campo `file` |
| POST   | `/api/files/upload/multiple` | Subir múltiples archivos | `multipart/form-data` → campo `files` |
| GET    | `/api/files` | Listar todos los archivos subidos | - |
| GET    | `/api/files/{id}` | Metadata del archivo | - |
| GET    | `/api/files/{id}/download` | **Descargar archivo** | Retorna el archivo con `Content-Disposition` |
| DELETE | `/api/files/{id}` | Eliminar archivo | - |

**Ejemplo curl - subir archivo:**
```bash
curl -X POST http://localhost:8080/api/files/upload \
  -F "file=@/ruta/al/archivo.pdf"
```

**Ejemplo curl - múltiples archivos:**
```bash
curl -X POST http://localhost:8080/api/files/upload/multiple \
  -F "files=@foto1.jpg" \
  -F "files=@documento.pdf"
```

---

### 🔁 Echo  `/api/echo`

Devuelve exactamente lo que recibe. **Ideal para verificar que tu cliente envía correctamente cada parte.**

| Método | URL | Descripción |
|--------|-----|-------------|
| GET    | `/api/echo` | Echo de query params + headers |
| GET    | `/api/echo/{segment}` | Echo de path variable |
| GET    | `/api/echo/{segment}/{sub}` | Echo de múltiples path variables |
| POST   | `/api/echo/body` | Echo de JSON body |
| POST   | `/api/echo/form` | Echo de `application/x-www-form-urlencoded` |
| POST   | `/api/echo/multipart` | Echo de `multipart/form-data` (fields + archivos) |
| PUT    | `/api/echo/{id}` | Echo de PUT |
| PATCH  | `/api/echo/{id}` | Echo de PATCH |
| DELETE | `/api/echo/{id}` | Echo de DELETE |
| HEAD   | `/api/echo/{id}` | HEAD (responde solo con headers) |
| OPTIONS| `/api/echo` | OPTIONS (retorna Allow header) |

---

### ⚙️ Misc  `/api/misc`

| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/misc/health` | Health check |
| GET | `/api/misc/status/{code}` | Responde con el status HTTP pedido (200, 404, 500…) |
| GET | `/api/misc/delay/{ms}` | Respuesta con delay artificial en ms |
| GET | `/api/misc/stream?count=10&intervalMs=500` | Server-Sent Events (streaming reactivo) |
| GET | `/api/misc/no-content` | Retorna 204 No Content |
| GET | `/api/misc/redirect` | Retorna 302 redirect |
| GET | `/api/misc/large-list?n=100` | Lista de N elementos (max 1000) |
| GET | `/api/misc/bearer` | Valida `Authorization: Bearer <token>` |
| GET | `/api/misc/basic` | Valida `Authorization: Basic <base64>` |
| GET | `/api/misc/api-key` | Valida header `X-API-Key` (debe empezar con `demo-`) |
| POST | `/api/misc/validate` | Fuerza validación y retorna errores 400 |

---

## Formato de respuesta estándar

```json
{
  "success": true,
  "message": "User created successfully",
  "data": { ... },
  "meta": { ... }
}
```

En errores de validación:
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "email": "Invalid email format",
    "name": "Name is required"
  }
}
```

---

## Bodies de ejemplo

**Crear usuario:**
```json
{
  "name": "Juan Pérez",
  "email": "juan@example.com",
  "age": 30,
  "active": true
}
```

**Crear producto:**
```json
{
  "name": "Laptop Ultra",
  "description": "Laptop de alto rendimiento",
  "price": 1499.99,
  "stock": 15,
  "category": "ELECTRONICS",
  "active": true
}
```

**PATCH usuario (solo campos a modificar):**
```json
{
  "age": 35,
  "active": false
}
```

---

## Ejecutar tests
```bash
mvn test
```
