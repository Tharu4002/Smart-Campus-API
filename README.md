# Smart Campus API

WAR-based JAX-RS coursework project for Tomcat 9 using `javax` and Jersey 2.x.

## Tech stack
- Java 17
- Maven
- Tomcat 9
- JAX-RS (Jersey 2.x)
- `javax.ws.rs.*`
- In-memory data structures only (`ConcurrentHashMap`, `ArrayList`)

## NetBeans setup
1. Open NetBeans.
2. File -> Open Project.
3. Select this Maven project folder.
4. Tools -> Servers -> Add Server -> Apache Tomcat or TomEE.
5. Point NetBeans to your Tomcat 9 installation.
6. Right click the project -> Properties -> Run.
7. Set Server to Tomcat 9.
8. Set Context Path to `/smart-campus-api`.
9. Clean and Build.
10. Run project.

## Build from terminal
```bash
mvn clean package
```

Output WAR:
```bash
target/smart-campus-api.war
```

## Base URLs
- Home: `http://localhost:8080/smart-campus-api/`
- Discovery: `http://localhost:8080/smart-campus-api/api/v1`
- Rooms: `http://localhost:8080/smart-campus-api/api/v1/rooms`
- Sensors: `http://localhost:8080/smart-campus-api/api/v1/sensors`

## Example curl commands
### 1. Discovery
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1
```

### 2. Create room
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "LAB-101",
    "name": "Computer Lab 101",
    "capacity": 60
  }'
```

### 3. Get all rooms
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms
```

### 4. Create sensor
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEMP-001",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 0.0,
    "roomId": "LAB-101"
  }'
```

### 5. Filter sensors by type
```bash
curl -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=Temperature"
```

### 6. Add a reading
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{
    "value": 26.7
  }'
```

### 7. Get sensor readings
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings
```

### 8. Delete room (will fail if room has sensors)
```bash
curl -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LAB-101
```

## Code structure
```text
src/main/java/com/smartcampus/
├── config
├── exception
├── filter
├── mapper
├── model
├── resource
└── store
```

## Notes
- Uses `@ApplicationPath("/api/v1")`.
- Uses sub-resource locator for `/sensors/{sensorId}/readings`.
- Includes custom exception mappers and request/response logging filter.
- Designed for coursework requirements using JAX-RS only.
