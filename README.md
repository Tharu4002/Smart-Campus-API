Q1: JAX-RS Resource Class Lifecycle
Question:
Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? How does this impact how you manage in-memory data structures?
Answer:
By default, JAX-RS uses a per-request lifecycle for resource classes. This means the JAX-RS runtime (Jersey in this case) creates a new instance of each resource class such as RoomResource or SensorResource for every incoming HTTP request. After the response is sent, that instance is discarded. This behaviour is defined in the JAX-RS specification and is different from a singleton approach.
This has an important effect on how in-memory data is managed. Since each request gets a separate resource object, you cannot store shared application data (like a list or map of rooms or sensors) inside instance variables of the resource class. If you do, the data will be recreated for every request and lost afterwards, meaning nothing will persist between requests.
To solve this problem, all shared data in this project is stored in a separate DataStore class using static fields backed by ConcurrentHashMap. Static fields belong to the class itself, not to individual instances, so they remain available across all requests for the entire lifetime of the application.
ConcurrentHashMap is used because JAX-RS can handle multiple requests at the same time using different threads. A normal HashMap is not thread-safe and can lead to data corruption or race conditions when accessed concurrently. ConcurrentHashMap allows safe read and write operations without needing explicit synchronization, making it suitable for a multi-threaded, high-performance server environment.

Q2: HATEOAS and Hypermedia in RESTful Design
Question:
Why is the provision of "Hypermedia" (HATEOAS) considered a hallmark of advanced RESTful design? How does it benefit client developers compared to static documentation?
Answer:
HATEOAS stands for Hypermedia as the Engine of Application State. It represents the highest level of RESTful design (Level 3 of the Richardson Maturity Model). The main idea is that API responses should not only return data, but also include links that guide the client on what actions can be taken next and where to go.
For example, when a client creates a new sensor, a HATEOAS-based response would return the sensor data along with links such as:
•	self (e.g., GET /sensors/TEMP-001) 
•	readings (e.g., /sensors/TEMP-001/readings) 
•	room (e.g., /rooms/LAB-101) 
This means the client does not need to guess or hard-code these URLs.
This approach provides several benefits for client developers. First, it creates loose coupling between the client and the server. If the server changes its URL structure, clients will still work as long as they follow the links provided in responses, instead of relying on fixed paths.
Second, it reduces the need to constantly check static documentation, because the API responses themselves show what actions are possible.
Third, it makes the API more self-discoverable, which is very useful for automated systems and third-party integrations.
In contrast, static documentation can easily become outdated and requires developers to manually check it against each API request.

Q3: Returning IDs vs Full Room Objects in a List
Question:
When returning a list of rooms, what are the implications of returning only IDs versus returning full room objects? Consider network bandwidth and client-side processing.
Answer:
There is an important trade-off between these two approaches.
Returning only IDs reduces the size of each response, which saves network bandwidth. This is especially useful when dealing with a large number of rooms (for example, thousands). However, the drawback is that the client must make an additional HTTP request for each room to get its full details. This leads to the “N+1 problem,” which increases total latency and also adds extra load on the server when full data is required.
On the other hand, returning full room objects in a single response means the client receives all required data in one request. This reduces the number of API calls, lowers latency, and makes client-side development simpler. The downside is that the response size becomes larger, which can be inefficient if the client only needs minimal information (such as just room names for a dropdown list).
In practice, the more common modern approach is to return full objects in the list response, as done in this implementation. The cost of a slightly larger response is usually much lower than making many additional API calls.
For very large datasets, techniques like pagination and field filtering (allowing clients to request only specific fields using query parameters) are used to balance performance and efficiency. Returning only IDs is mainly useful in cases where bandwidth is very limited or when the list is used only for reference rather than display

Q4: Is DELETE Idempotent?
Question:
Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client sends the same DELETE request for a room multiple times.
Answer:
Yes, the DELETE operation is considered idempotent according to the HTTP specification. This means that sending the same request multiple times should result in the same final state on the server as sending it once. However, the response returned to the client can be different, which is still valid under the definition of idempotency.
In this implementation:
When the DELETE request is first sent to /api/v1/rooms/LAB-101, the room exists in the system. The request passes the safety check (ensuring there are no sensors in the room), and the room is removed from the DataStore. The server then returns 200 OK with a success message.
When the same DELETE request is sent again, the room no longer exists. The DataStore.get() method returns null, which leads to a NotFoundException. In this case, the server returns 404 Not Found.
Even though the responses are different, the final server state is the same in both cases the room does not exist in the system. This is what makes the operation idempotent.
The change in HTTP status code (from 200 to 404) does not break idempotency, because idempotency refers only to the effect on the server state, not the response returned. This behaviour is also aligned with RFC 9110, which states that a repeated DELETE request on a non-existent resource can return a 404 response.


Q5: Consequences of @Consumes (MediaType.APPLICATION_JSON) Mismatch
Question:
Explain the technical consequences if a client sends data in a format other than application/json (e.g., text/plain or application/xml) to a method annotated with @Consumes(MediaType.APPLICATION_JSON).
Answer:
The annotation @Consumes(MediaType.APPLICATION_JSON) tells the JAX-RS runtime that this resource method only accepts requests where the Content-Type header is application/json. If a client sends a request with a different content type, such as text/plain or application/xml, the mismatch is handled before the method is executed.
In this case, Jersey (the JAX-RS implementation used) checks the incoming Content-Type during request processing. If it does not match any resource method that supports it, the framework immediately rejects the request and returns an HTTP 415 Unsupported Media Type response.
This means the actual resource method is never called, and no application logic is executed. The rejection happens at the routing level within the framework itself.
This behaviour is important because it enforces a strict contract between the client and the server. It prevents the application from processing invalid or unexpected data formats, which could otherwise lead to errors such as NullPointerException or deserialization failures later in the execution flow.
It also improves clarity for API users. A 415 response clearly indicates that the client is sending the wrong format, instead of the server failing later with a confusing 500 Internal Server Error

Q6: @QueryParam vs Path Segment for Filtering
Question:
You implemented sensor type filtering using @QueryParam. Contrast this with using a path segment (e.g., /api/v1/sensors/type/CO2). Why is the query parameter approach generally superior for filtering?
Answer:
Both the @QueryParam approach (GET /api/v1/sensors?type=CO2) and the path-segment approach (GET /api/v1/sensors/type/CO2) can be used for filtering, but they follow different REST design principles.
In REST, path segments are meant to represent resource identity. They are used to identify specific resources or sub-resources. For example, /api/v1/sensors/TEMP-001 is correct because TEMP-001 uniquely identifies a sensor. However, using /type/CO2 in the path suggests that “CO2” is a resource on its own, which it is not it is just a property used for filtering.
On the other hand, query parameters are designed for filtering, searching, sorting, and pagination. They modify how a collection is returned without changing the resource itself. For example, both /api/v1/sensors and /api/v1/sensors?type=CO2 refer to the same sensors resource—the query parameter simply narrows the results.
The query parameter approach is also more flexible in practice. It allows multiple filters to be combined easily, such as:
/api/v1/sensors?type=CO2&status=ACTIVE
This would be difficult and messy to represent using path segments, as it would require complex and rigid URL structures.
Additionally, query parameters are more standard and widely supported. API tools, HTTP caching systems, and search engines all treat query parameters as filters by convention. In contrast, using path segments for filtering can lead to deeply nested and less maintainable URL designs, which go against the idea that paths should represent a stable resource hierarchy.

Q7: Architectural Benefits of the Sub-Resource Locator Pattern
Question:
Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity compared to defining all nested paths in one controller?
Answer:
The Sub-Resource Locator pattern in JAX-RS is a design approach where a resource method does not directly handle an HTTP request. Instead, it returns an instance of another class that is responsible for handling the remaining part of the request path. In this project, for example, SensorResource delegates the path /{sensorId}/readings to a SensorReadingResource instance.
The main advantage of this pattern is separation of concerns. Each resource class has a clear and specific responsibility. SensorResource handles general sensor operations like create, update, and delete, while SensorReadingResource focuses only on reading-related operations for a specific sensor. This makes each class smaller, easier to understand, and easier to test independently.
In large applications with many nested endpoints, placing all logic inside a single controller can lead to a large, complex “god class” that is difficult to maintain. The Sub-Resource Locator pattern avoids this by allowing the system to scale more cleanly. New functionality can be added by creating new sub-resource classes and linking them through locator methods, without modifying existing controllers.
Another benefit is context passing and reuse of logic. In this design, SensorReadingResource receives the sensorId through its constructor. This ensures all methods automatically operate within the correct sensor context. As a result, there is no need to repeatedly validate or retrieve the parent sensor in every method.
Instead, the validation is done once in the SensorResource locator method (to confirm the sensor exists), and then control is passed to the sub-resource. This creates a cleaner, more structured flow and reduces code duplication compared to handling all nested validation inside a single class.

Q8: Why HTTP 422 is More Semantically Accurate Than 404
Question:
Why is HTTP 422 (Unprocessable Entity) often considered more semantically accurate than 404 (Not Found) when a POST request contains a roomId that does not exist?
Answer:
The key difference depends on what is actually “not found”.
HTTP 404 Not Found means that the resource identified by the request URL does not exist. For example, if a client sends GET /api/v1/rooms/GHOST-ROOM and that room does not exist, then returning 404 is correct because the URL itself refers to a missing resource.
However, in the case of a request like POST /api/v1/sensors, the URL /api/v1/sensors is valid and exists. The problem is not the endpoint. Instead, the issue is inside the request body, for example when "roomId": "GHOST-ROOM" is provided.
In this case, the JSON is properly formatted and valid, but it is semantically incorrect, because it refers to a room that does not exist in the system.
HTTP 422 Unprocessable Entity is designed for exactly this situation. It means the server understood the request format and successfully parsed it, but could not process it because the data itself is logically invalid.
Using 422 gives a clearer message to the client: the request is well-formed, but one of the values is invalid or refers to missing data. A 404 response here would be misleading, because it could incorrectly suggest that the endpoint URL is wrong, when the real issue is inside the request payload.


Q9: Cybersecurity Risks of Exposing Java Stack Traces
Question:
From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather?
Answer:
Exposing raw Java stack traces to external users is a serious security risk known as information disclosure or verbose error reporting. Stack traces are meant only for internal debugging by developers, not for public users.
If an attacker gains access to a stack trace, they can extract several types of sensitive information:
1. Technology fingerprinting:
The stack trace can reveal details about the system’s technology stack, such as the Java version, the JAX-RS implementation (e.g., Jersey 2.39.1), the servlet container (e.g., Tomcat 9), and other libraries used. With this information, an attacker can search for known vulnerabilities (CVEs) in those specific versions and attempt targeted attacks.
2. Internal package and class structure:
Stack traces expose internal class names and package structures (e.g., com.smartcampus.resource.RoomResource). This gives attackers insight into how the application is designed and helps them understand business logic and identify potential weak points or injection areas.
3. Source code structure clues:
Line numbers in stack traces can reveal where errors occur in the code. Combined with class and method names, this allows attackers to make educated guesses about how the system works and how data flows through the application.
4. File system information:
In some cases, stack traces may also include absolute file paths from the server. This can expose directory structures and help attackers plan further attacks such as path traversal or locating sensitive files.
To prevent these risks, this project uses a GlobalExceptionMapper<Throwable> that catches all unhandled exceptions. Instead of exposing internal details, it returns a generic 500 Internal Server Error response in JSON format. All technical details are kept in server logs only, ensuring that sensitive implementation information is not exposed to external users.


Q10: Why Use JAX-RS Filters for Cross-Cutting Concerns Like Logging?
Question:
Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() calls inside every resource method?
Answer:
A cross-cutting concern refers to functionality that applies across many parts of an application, such as logging, authentication, CORS handling, and rate limiting. Implementing these manually inside every resource method goes against the DRY (Don't Repeat Yourself) principle and creates several issues.
1. Consistency and correctness:
If logging is added manually in each method, it is easy to forget it in some places or to log information in different formats across different methods. A JAX-RS filter automatically intercepts every request and response, ensuring consistent logging across the entire application with no risk of missing any endpoint.
2. Maintainability:
If the logging format needs to change (for example, adding a timestamp or correlation ID), a filter allows you to update it in one place. With manual logging, every resource method would need to be updated individually, which is time-consuming and error-prone.
3. Separation of concerns:
Resource classes should focus only on business logic, such as creating sensors or retrieving rooms. Adding logging code inside them mixes responsibilities and makes the code harder to read, test, and maintain. Filters keep logging separate from business logic.
4. Framework-level design:
JAX-RS provides filters as a built-in feature specifically for handling cross-cutting concerns. Using @Provider along with ContainerRequestFilter and ContainerResponseFilter integrates logging into the request lifecycle in a clean and standard way that is widely recognised in JAX-RS applications.
In this project, the LoggingFilter class demonstrates this approach clearly. It logs the HTTP method and URI for every incoming request, and the response status code for every outgoing response, without being directly coupled to any specific resource class.

