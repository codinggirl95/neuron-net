# CacheFlow — In-Memory Cache CRUD API (Spring Boot)

A small Spring Boot app exposing **POST / GET / PUT / PATCH / DELETE** endpoints for a simple **in-memory key–value store**.  
It uses a `ConcurrentHashMap` backend and supports **JSON merge-patch** (`PATCH`) for partial updates.

---

## 🚀 Features

- **Create** (`POST`) — Add a new key/value (fails if key exists)
- **Read** (`GET`) — Retrieve value by key (auto-detects JSON/plain text)
- **Replace** (`PUT`) — Replace or insert value
- **Merge-Patch** (`PATCH`) — Merge JSON objects
- **Delete** (`DELETE`) — Remove a key/value
See attached postman for samples
---

## 🛠 Tech Stack

- **Java 17**
- **Spring Boot 3.3.2**
- **spring-boot-starter-web**
- **spring-boot-starter-validation** (Jakarta Validation)
- (Optional) **Lombok** for logging & boilerplate reduction

---

## 📂 Project Structure
com.myapp.cacheflow
├─ CacheflowApplication.java # @SpringBootApplication
├─ api
│ └─ CacheController.java # REST endpoints
└─ service
└─ InMemoryCache.java # ConcurrentHashMap-backed store

## ▶️ Running the App

```bash
# Run directly
mvn spring-boot:run

# Or package and run
mvn clean package
java -jar target/cacheflow-1.0.0.jar
```
---

## 📡 API Endpoints

### Create (POST)
```http
POST /cache/{key}
Content-Type: application/json
```
- **201 Created** if added
- **409 Conflict** if key exists

Example:
```bash
curl -i -X POST localhost:8080/cache/user123   -H "Content-Type: application/json"   -d '{"name":"Ava","age":30}'
```

---

### Read (GET)
```http
GET /cache/{key}
```
- **200 OK** (JSON or plain text)
- **404 Not Found** if missing

Example:
```bash
curl -i localhost:8080/cache/user123
```

---

### Replace / Upsert (PUT)
```http
PUT /cache/{key}
Content-Type: application/json
```
- **200 OK** (always replaces or creates)

Example:
```bash
curl -i -X PUT localhost:8080/cache/user123   -H "Content-Type: application/json"   -d '{"name":"Ava","age":31}'
```

---

### Merge-Patch (PATCH)
```http
PATCH /cache/{key}
Content-Type: application/json
```
- RFC-7386 style JSON merge:
    - Merge nested fields
    - `null` deletes a field
    - Non-JSON → full replace

Example:
```bash
curl -i -X PATCH localhost:8080/cache/user123   -H "Content-Type: application/json"   -d '{"age":32,"extra":{"likes":"coffee"}}'
```

---

### Delete (DELETE)
```http
DELETE /cache/{key}
```
- **204 No Content** if deleted
- **404 Not Found** if missing

Example:
```bash
curl -i -X DELETE localhost:8080/cache/user123
```

---

## 🧪 Quick Test Script
```bash
curl -s -X POST localhost:8080/cache/k1 -H "Content-Type: application/json" -d '"v1"'
curl -s localhost:8080/cache/k1
curl -s -X PUT localhost:8080/cache/k2 -H "Content-Type: application/json" -d '{"a":1,"b":{"x":10}}'
curl -s -X PATCH localhost:8080/cache/k2 -H "Content-Type: application/json" -d '{"b":{"y":20},"c":null}'
curl -i -X DELETE localhost:8080/cache/k1
```

## 🔮 Future Enhancements

- **TTL & Expiry:** support `?ttlSeconds=` on POST/PUT; add default per-namespace TTL.
- **Namespaces & Key Patterns:** `/cache/{ns}/{key}` with list/prefix-scan endpoints.
- **Metrics & Tracing:** Micrometer, Prometheus, OpenTelemetry spans around cache ops.
- **Auth & Rate Limits:** API keys, per-IP quotas, and burst limiting (Bucket4j).
- **Bulk APIs:** batch GET/PUT/DELETE, streaming export/import.
- **Validation & Schemas:** optional JSON Schema per namespace.
- **Sharding:** consistent hashing if you later distribute across nodes.
- **Parallel Processing:** Make updates to documents in parallel 
---