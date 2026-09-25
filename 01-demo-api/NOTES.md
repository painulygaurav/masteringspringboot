# Spring Boot Notes

## Table of Contents
1. [Project Setup](#project-setup)
2. [The Embedded Server](#the-embedded-server)
3. [Servlets: Before Spring Boot](#servlets-before-spring-boot)
4. [DispatcherServlet](#dispatcherservlet)
5. [Logging](#logging)
6. [Tomcat Thread Model](#tomcat-thread-model)
7. [Tomcat NIO Internals](#tomcat-nio-internals)
8. [Q&A](#qa)
9. [MVC and Request Mapping](#mvc-and-request-mapping)
10. [How DispatcherServlet Finds the Right Controller](#how-dispatcherservlet-finds-the-right-controller)
11. [Reflection](#reflection)
12. [RequestResponseBodyMethodProcessor](#requestresponsebodymethodprocessor)
13. [HTTP/1.1 vs HTTP/2](#http11-vs-http2)
14. [HTTP/2 vs gRPC](#http2-vs-grpc)
15. [Head-of-Line (HOL) Blocking](#head-of-line-hol-blocking)
16. [Handling Multiple Requests](#handling-multiple-requests)
17. [Who Creates the Controller Object?](#who-creates-the-controller-object)
18. [Up Next](#up-next)

---

## Project Setup

We created a new Spring Boot application with two dependencies:
1. `spring-boot-starter-web`
2. `lombok`

When the application starts, we see the following log line:
```
Tomcat initialized with port 8080 (http)
```

By default, Spring Boot starts the app server on **port 8080**.

---

## The Embedded Server

To understand the embedded server, it helps to think in terms of the OSI networking model:

- **Internet layer** — attaches the IP address.
- **Transport layer** — attaches the port (`8080`).

A **port number** is a 16-bit logical identifier (0–65,535) that tells the operating system which application or service should receive incoming or outgoing network data.

The Spring Web dependency ships with an **embedded Tomcat server**, which runs and continuously listens for client requests on a specific port.

### How the embedded server gets included

```
spring-boot-starter-web
  → spring-boot-starter-tomcat
      → tomcat-embed-core
      → tomcat-embed-el
      → tomcat-embed-websocket
```

Embedded servers are also called **embedded containers**. Besides Tomcat, another common option is **Jetty**.

- **Tomcat** is the actual server.
- **`spring-boot-starter-tomcat`** is an abstraction layer created by the Spring Boot developers to collate the boilerplate code related to servlets and socket connections, so developers don't need to write that plumbing themselves and can focus on business logic.
- Tomcat runs continuously based on a **heartbeat** mechanism.

---

## Servlets: Before Spring Boot

Before Spring Boot, a client request was handled by a servlet you wrote and registered yourself.

### A plain servlet

```java
import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class HelloWorldServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body><h1>Hello, World!</h1></body></html>");
    }
}
```

Running it: `http://localhost:8080/HelloWorldServlet/hello`

### XML-based configuration (`web.xml`)

To deploy a servlet, you had to configure it in `web.xml`, mapping URLs to servlet classes:

```xml
<web-app xmlns="http://www.oracle.com/webfolder/technetwork/jsc/xml/ns/javaee/index.html"
         xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.oracle.com/webfolder/technetwork/jsc/xml/ns/javaee/index.html
         http://www.oracle.com/webfolder/technetwork/jsc/xml/ns/javaee/index.html/web-app_3_0.xsd"
         version="3.0">

    <servlet>
        <servlet-name>HelloWorldServlet</servlet-name>
        <servlet-class>HelloWorldServlet</servlet-class>
    </servlet>

    <servlet-mapping>
        <servlet-name>HelloWorldServlet</servlet-name>
        <url-pattern>/hello</url-pattern>
    </servlet-mapping>

</web-app>
```

### Annotation-based configuration (modern approach)

From Servlet 3.0 onward, servlets can be configured with the `@WebServlet` annotation instead of `web.xml`:

```java
@WebServlet("/hello")
public class HelloWorldServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body><h1>Hello, World!</h1></body></html>");
    }
}
```

> Note: to compile and run this code, you need a JVM.

---

## DispatcherServlet

In Spring Boot, every incoming request is intercepted by `DispatcherServlet`, which listens on port 8080. As soon as it receives a request, it dispatches it to the appropriate Java class.

Spring Boot's `DispatcherServlet` eliminates the need to create and register a separate servlet by hand — no more boilerplate.

After `DispatcherServlet` is set up, the following bootstrap code runs:

```java
@SpringBootApplication
public class DemoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApiApplication.class, args);
    }

}
```

---

## Logging

You can control logging verbosity via `src/main/resources/application.properties`:
```
logging.level.root=trace
```

### Logging levels (from least to most verbose)
- `ERROR`
- `WARN`
- `INFO`
- `DEBUG`
- `TRACE` — logs everything

`TRACE` is **not recommended in production** because it consumes excessive disk/database space.

### Anatomy of a log line

```
2026-09-24T21:17:58.056+05:30 TRACE 78219 --- [demo-api] [           main] o.s.b.f.s.DefaultListableBeanFactory     : Returning cached instance of singleton bean 'applicationAvailability'
```

| Segment | Meaning |
|---|---|
| `78219` | **PID** — process ID, unique per process |
| `[demo-api]` | Project name |
| `[main]` | Thread name — the main thread |
| `o.s.b.f.s.DefaultListableBeanFactory` | Abbreviated package + class name (space-limited log format) |

The thread name changes over the application's lifecycle, e.g.:
- `[nio-8080-Poller]` — a thread handling I/O polling
- `[ionShutdownHook]` — the shutdown hook thread, active just before the app stops. This is where graceful shutdown happens: running garbage collection, cleaning up memory/files, and shutting down Tomcat gracefully. As soon as you hit "stop," you'll start seeing logs tagged with this thread.

Example shutdown logs:
```terminaloutput
2026-09-24T21:30:55.400+05:30 DEBUG 78219 --- [demo-api] [ionShutdownHook] o.s.b.a.ApplicationAvailabilityBean      : Application availability state ReadinessState changed from ACCEPTING_TRAFFIC to REFUSING_TRAFFIC
2026-09-24T21:30:55.400+05:30 DEBUG 78219 --- [demo-api] [ionShutdownHook] ConfigServletWebServerApplicationContext : Closing org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext@1e7aa82b, started on Thu Sep 24 21:17:57 IST 2026
2026-09-24T21:30:55.403+05:30 TRACE 78219 --- [demo-api] [ionShutdownHook] o.s.b.f.s.DefaultListableBeanFactory     : Returning cached instance of singleton bean 'applicationTaskExecutor'
2026-09-24T21:30:55.403+05:30 TRACE 78219 --- [demo-api] [ionShutdownHook] o.s.b.f.s.DefaultListableBeanFactory     : Returning cached instance of singleton bean 'springApplicationAdminRegistrar'
2026-09-24T21:30:55.404+05:30 TRACE 78219 --- [demo-api] [ionShutdownHook] o.s.b.f.s.DefaultListableBeanFactory     : Returning cached instance of singleton bean 'applicationTaskExecutor'
2026-09-24T21:30:55.404+05:30 TRACE 78219 --- [demo-api] [ionShutdownHook] o.s.b.f.s.DefaultListableBeanFactory     : Returning cached instance of singleton bean 'lifecycleProcessor'
2026-09-24T21:30:55.404+05:30 TRACE 78219 --- [demo-api] [ionShutdownHook] o.s.b.f.s.DefaultListableBeanFactory     : Returning cached instance of singleton bean 'springBootLoggingLifecycle'
```

This is the *default* log format — developers can define their own custom format.

---

## Tomcat Thread Model

### Request flow, high level

```
Browser
  ↓
HTTP :8080
  ↓
Tomcat Acceptor
  ↓
nio-8080-Poller
  ↓
Tomcat request-processing thread
  ↓
DispatcherServlet
  ↓
Controller
  ↓
Service
  ↓
Repository
```

### Non-daemon threads

Tomcat starts non-daemon threads such as:
```
http-nio-8080-Acceptor
http-nio-8080-Poller
http-nio-8080-exec-1
http-nio-8080-exec-2
...
```

As long as these threads are running, the JVM stays alive.

### Startup sequence

```
Tomcat starts
     ↓
StandardServer.start()   — starts the Tomcat server and its configured services
     ↓
Service.start()          — starts the service components, including its connectors
     ↓
Connector.start()        — starts the HTTP connector and its protocol handler
     ↓
NioEndpoint.start()       — starts the NIO networking infrastructure (acceptor + poller threads)
     ↓
Acceptor + Poller threads start — accept incoming TCP connections on the server port
     ↓
Non-daemon threads remain alive
     ↓
JVM stays alive
```

### Threads that keep the JVM alive

```
NioEndpoint
├── Acceptor        — waits for new TCP connections
├── Poller          — waits for I/O events on established connections
└── Executor/worker — processes HTTP requests
    ├── http-nio-8080-exec-1
    ├── http-nio-8080-exec-2
    └── ...
```

---

## Tomcat NIO Internals

In Tomcat's NIO implementation, the **Poller** waits for network I/O events on `SocketChannel`s — specifically:

- **READ** — data has arrived from the client and can be read.
- **WRITE** — the socket is ready to accept outgoing data.
- **CONNECT** — a connection has completed (more relevant for client-side channels).
- **ACCEPT** — a new connection is available (normally handled by the Acceptor, not the Poller).

For a Tomcat HTTP server, **READ** and **WRITE** are the important ones.

```
Client
   │  HTTP request
   ▼
SocketChannel
   │  "data available to read"
   ▼
Selector — the Poller registers sockets with interest ops like SelectionKey.OP_READ / .OP_WRITE
   ▼
Poller
   ▼
SocketProcessor
   ▼
HTTP processing
```

The Poller is essentially asking the OS: *"Tell me when any of these thousands of sockets are ready for I/O."* The `Selector` blocks until one or more channels become ready.

### Example: an HTTP request arriving

Suppose the browser sends:
```http request
GET /hello HTTP/1.1
Host: localhost:8080
```

```
Browser
   │  TCP data arrives
   ▼
SocketChannel
   │  OS marks socket readable
   ▼
Selector.select()
   │  returns SelectionKey with OP_READ
   ▼
Poller
   ▼
SocketProcessor
   ▼
Tomcat HTTP parser
   ▼
Spring DispatcherServlet
```

So when you see code like:
```java
selector.select();
```
that's essentially where the Poller's thread sleeps until the OS reports that something interesting happened on one of the network connections.

---

## Q&A

**Q: If dependencies for Tomcat, Jetty, and Undertow all exist, which one takes precedence?**

A: Tomcat takes precedence because it's the default. If Jetty and Undertow are present instead of Tomcat, Jetty would run next. The priority order is defined by the `spring-boot-starter-web` dependency chain.

**Q: How does `DispatcherServlet` find the right controller to serve a request?**

A: See [How DispatcherServlet Finds the Right Controller](#how-dispatcherservlet-finds-the-right-controller) below.

---

## MVC and Request Mapping

```
MVC → Model View Controller

Model      — represents a real-world entity
View       — presentation of data returned from the controller
Controller — entry point for all client requests
```

### Anatomy of an HTTP request
1. **DNS** — resolves the hostname to an IP address
2. **Endpoint**
3. **Request headers**
4. **Request parameters**
5. **Body**
6. **Request method**

```
http request → DispatcherServlet → looks for Controllers
                                    (e.g. CartController, PayController, AccountController)
```

Controllers register themselves with `DispatcherServlet`.

---

## How DispatcherServlet Finds the Right Controller

`DispatcherServlet` does **not** scan all controllers for every request. Spring builds a mapping table at startup, and `DispatcherServlet` uses that table to find the matching controller method.

### Step 1 — Spring discovers your controller at startup

During startup, Spring sees:
```java
@RestController
public class UserController
```
and creates a Spring bean for it. It also sees:
```java
@GetMapping("/users/{id}")
```
and registers that mapping. The key class responsible for maintaining this mapping is:
```java
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
```

### Step 2 — the request reaches DispatcherServlet

```
Tomcat
   ↓
DispatcherServlet
```

The important method here is:
```java
DispatcherServlet.doDispatch(request, response)
```
This is the heart of Spring MVC request processing.

### Step 3 — DispatcherServlet asks HandlerMapping

Inside `doDispatch()`, Spring essentially asks: *"Find me a handler for this request."* It delegates to `getHandler(request)`, which consults its configured `HandlerMapping`. For annotation-based controllers, this is typically `RequestMappingHandlerMapping`, which checks:

- HTTP method
- URL/path
- Headers
- Query parameters
- `consumes` / `produces`

For `GET /users/123`, it finds `UserController.getUser()`.

### Step 4 — path variables are extracted

### Step 5 — HandlerAdapter invokes the method

`DispatcherServlet` doesn't call `UserController.getUser()` directly — it uses a `HandlerAdapter`. For annotated controller methods, that's `RequestMappingHandlerAdapter`.

```
DispatcherServlet
      │  find handler
      ▼
RequestMappingHandlerMapping
      │
      ▼
UserController.getUser()
      │  HandlerMethod
      ▼
RequestMappingHandlerAdapter
      │
      ▼
invoke Java method
```

### Full picture, end to end

```
Browser
   ↓
TCP Socket
   ↓
Tomcat NioEndpoint
   ↓
Poller
   ↓
Tomcat HTTP processing
   ↓
DispatcherServlet
   ↓
RequestMappingHandlerMapping
   ↓
Controller method
   ↓
Service
   ↓
Repository/DB
```

Before Spring Boot, developers had to define a config file listing controller mappings by hand. Spring Boot replaces that with annotations, which work internally via **reflection**.

---

## Reflection

**Reflection in Java** is the ability of a program to inspect and interact with classes, methods, fields, constructors, and annotations at runtime — even for members you didn't explicitly code against. Think of it as *Java code examining Java code while the program is running.*

### Without reflection

You call a method directly, and the compiler knows about the class/method at compile time:
```java
User user = new User();
user.getName();
```

### With reflection

You discover the class and method at runtime:
```java
Class<?> clazz = User.class;
Method method = clazz.getMethod("getName");
Object result = method.invoke(user);
```
Here, `"getName"` is just a string — Java discovers the actual method at runtime.

### What can reflection inspect?

```java
Class<?> clazz = User.class;

clazz.getMethods();          // methods
clazz.getFields();           // fields
clazz.getConstructors();     // constructors
clazz.getAnnotations();      // annotations
clazz.getDeclaredMethods();  // all declared methods
```

For a class:
```java
class User {
    public void getName() {}
    public void save() {}
}
```
reflection can discover `getName` and `save` without you knowing those method names beforehand.

### Why frameworks use reflection

Reflection is one of the foundations of frameworks like **Spring**. Given:
```java
@RestController
public class UserController {

    @GetMapping("/users")
    public List<User> getUsers() {
        ...
    }
}
```
Spring inspects the class via reflection to answer:
```
"Is this a Controller?"
       ↓
"Does this method have @GetMapping?"
       ↓
"What URL does it map to?"  →  "/users"
```
and then builds the mapping `GET /users → UserController.getUsers()` — all without you manually registering every controller.

### Reflection vs. normal Java

| Normal Java | Reflection |
|---|---|
| Your code → known class → known method → compile time | Your code → `Class` object → discover class/method → inspect metadata → invoke at runtime |

### Important reflection classes

```java
java.lang.Class
java.lang.reflect.Method
java.lang.reflect.Field
java.lang.reflect.Constructor
```

```java
Method method = clazz.getMethod("getName");
method.invoke(user);
```

- `Class` — represents a class at runtime
- `Method` — represents a method
- `Field` — represents a field
- `Constructor` — represents a constructor
- `invoke()` — actually calls the discovered method

### How this connects to Spring

```
Your Controller
      ↓
Spring scans it
      ↓
Reflection
      ↓
Find @GetMapping
      ↓
Create mapping
      ↓
GET /users → UserController.getUsers()
      ↓
HTTP request arrives
      ↓
Spring finds the mapping
      ↓
Reflection / invocation machinery
      ↓
getUsers()
```

> **Key takeaway:** Reflection lets Java discover and interact with code at runtime instead of requiring every class, method, and field to be hard-coded at compile time.

---

## RequestResponseBodyMethodProcessor

Example log trace:
```terminaloutput
2026-09-25T18:07:51.841+05:30 DEBUG 73267 --- [demo-api] [nio-8080-exec-8] o.s.web.servlet.DispatcherServlet        : GET "/person/all", parameters={}
2026-09-25T18:07:51.844+05:30 DEBUG 73267 --- [demo-api] [nio-8080-exec-8] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped to com.example.demoapi.PersonController#getAll()
2026-09-25T18:07:51.848+05:30 DEBUG 73267 --- [demo-api] [nio-8080-exec-8] m.m.a.RequestResponseBodyMethodProcessor : Using 'application/json;q=0.8', given [text/html, application/xhtml+xml, image/avif, image/webp, image/apng, application/xml;q=0.9, */*;q=0.8, application/signed-exchange;v=b3;q=0.7] and supported [application/json, application/*+json]
2026-09-25T18:07:51.848+05:30 DEBUG 73267 --- [demo-api] [nio-8080-exec-8] m.m.a.RequestResponseBodyMethodProcessor : Writing [[com.example.demoapi.Person@1d1f2fdc, com.example.demoapi.Person@5aa945e, com.example.demoapi.Person (truncated)...]
2026-09-25T18:07:51.850+05:30 DEBUG 73267 --- [demo-api] [nio-8080-exec-8] o.s.web.servlet.DispatcherServlet        : Completed 200 OK
```

`RequestResponseBodyMethodProcessor` is a core Spring MVC class that handles two related jobs for `@RestController` / `@ResponseBody`-style methods: reading request bodies into Java objects, and writing Java objects back out as response bodies. It's the class that makes `@RequestBody` and `@ResponseBody` actually work.

### What it does, concretely

1. **Argument resolution (`@RequestBody`)** — it implements `HandlerMethodArgumentResolver`. When a controller method parameter is annotated `@RequestBody`, this class reads the incoming HTTP request body (e.g. JSON) and deserializes it into the target Java type, using a registered `HttpMessageConverter` (commonly Jackson's `MappingJackson2HttpMessageConverter` for JSON).

2. **Return value handling (`@ResponseBody` / `@RestController`)** — it implements `HandlerMethodReturnValueHandler`. When a controller method returns a value and the class/method is annotated `@ResponseBody` (implied by `@RestController` for every method), this class writes that return value to the HTTP response body, again via an `HttpMessageConverter`.

### How the log lines map to behavior

- The **content negotiation** step (`Using 'application/json;q=0.8', given [...]`) happens inside this class's return-value handling — it negotiates which `MediaType` to serialize the response as, based on the `Accept` header and available converters.
- The **"Writing [...]"** line is this class invoking the chosen `HttpMessageConverter.write()` to serialize the `List<Person>` and stream it into the response body.

### Why it matters

It's the glue between your controller method's Java return type and the actual bytes sent over HTTP. Without it, `@RestController` methods wouldn't automatically produce JSON (or XML, etc.) — you'd have to manually write to the response yourself. It picks the converter, negotiates the format, and delegates serialization/deserialization to Jackson (or whichever matching converter is registered).

It lives in `org.springframework.web.servlet.mvc.method.annotation`, alongside similar processors for other annotation types (`RequestParamMethodArgumentResolver`, `PathVariableMethodArgumentResolver`, etc.) — Spring MVC uses a whole family of these resolver/handler classes to wire annotated controller methods into the servlet request/response lifecycle.

---

## HTTP/1.1 vs HTTP/2

| Feature | HTTP/1.1 | HTTP/2 |
|---|---|---|
| **Connections** | Multiple parallel TCP connections | Single connection, multiplexed |
| **Data format** | Text-based | Binary framing |
| **Head-of-line blocking** | Yes | No (at the HTTP layer) |
| **Header compression** | None (plain text) | HPACK compression |
| **Server push** | Not supported | Supported (largely deprecated now) |
| **Prioritization** | Implicit | Explicit stream priority |
| **Encryption** | Optional | Effectively mandatory (browsers require TLS) |

**Bottom line:** HTTP/2 is faster mainly because it multiplexes many requests over one connection and compresses headers, avoiding HTTP/1.1's need for multiple connections and repeated plain-text headers.

---

## HTTP/2 vs gRPC

They're not the same kind of thing — **gRPC is built on top of HTTP/2**.

| | HTTP/1.1 / HTTP/2 | gRPC |
|---|---|---|
| **What it is** | Transport-layer protocol | RPC framework (application layer) |
| **Runs on** | TCP (HTTP/1.1) or TCP+TLS (HTTP/2) | Requires HTTP/2 as its transport |
| **Data format** | Text (1.1) or binary frames (2.0); body can be anything (JSON, XML, etc.) | **Protocol Buffers (protobuf)** — binary, strongly typed |
| **Usage style** | Request/response over URLs (REST-style APIs typically use HTTP/1.1 or HTTP/2) | Defines services/methods via `.proto` files, generates client/server stubs |
| **Streaming** | HTTP/2 supports multiplexed streams, but semantics are built on top | Native **client streaming, server streaming, and bidirectional streaming** as first-class RPC patterns |
| **Browser support** | Full (both versions) | Limited — browsers can't speak native gRPC directly; needs a **gRPC-Web** proxy layer |

Enabling HTTP/2 in a Spring Boot app is about making existing REST endpoints faster/more efficient. To build **gRPC services** instead, you'd use a different stack entirely (`grpc-spring-boot-starter`, `.proto` files, generated stubs) — HTTP/2 comes along for free as the transport, but you wouldn't be writing `@RestController` methods anymore.

---

## Head-of-Line (HOL) Blocking

**What it is:** when one slow or stuck request/packet blocks everything queued behind it, even though those later items are otherwise ready to be processed.

### In HTTP/1.1
- With pipelining over a single connection, responses must come back **in the same order they were sent**.
- If request #1 is slow, requests #2, #3, etc. must wait, even if the server already finished processing them.
- **Workaround:** browsers open multiple parallel TCP connections (~6 per host) so one slow request doesn't block all others — at the cost of extra connection setup and TLS handshake overhead.

### In HTTP/2
- Solves HOL blocking **at the HTTP layer** via multiplexing — multiple independent streams share one TCP connection, and a slow stream doesn't block others.
- **But** HTTP/2 still runs over **TCP**, which guarantees in-order byte delivery. If a single TCP packet is lost, **all streams on that connection stall** until it's retransmitted — HOL blocking at the **transport layer**, which HTTP/2 can't avoid.

### In HTTP/3
- Moves off TCP entirely, running over **QUIC** (UDP-based) instead.
- QUIC handles streams independently at the transport level, so a lost packet only stalls the one stream it belonged to, not the whole connection.
- This is the main reason HTTP/3 exists: it fixes the transport-layer HOL blocking that HTTP/2 still had.

### Summary

| Protocol | Layer where HOL blocking is fixed |
|---|---|
| HTTP/1.1 | Not fixed (multiple connections is a workaround) |
| HTTP/2 | Fixed at the HTTP layer, **not** the TCP layer |
| HTTP/3 | Fixed at both layers (via QUIC/UDP) |

### Key point

HTTP/2 is the transport gRPC depends on — gRPC couldn't offer its streaming/multiplexing features without HTTP/2's multiplexed binary framing underneath. A typical REST API (like a Spring Boot `@RestController` returning JSON) can run over either HTTP/1.1 or HTTP/2 — the choice of transport doesn't change the JSON payloads. gRPC, specifically, requires HTTP/2 — it cannot run over HTTP/1.1.

---

## Handling Multiple Requests

If the server keeps getting hit with REST calls, the executor thread handling them may shift — e.g. `nio-8080-exec-1` may hand off to `nio-8080-exec-2` — and these worker threads get reused as needed.

---

## Who Creates the Controller Object?

Question: who creates the object for `PersonController`? We never call `new PersonController()`, yet its `getAll()` method still gets invoked.

The object is created on the **main thread** — we can see the default constructor being called, meaning the object is created well before the application (and any requests) exist.

This happens because of the `@RestController` annotation:

- `@RestController` inherits from `@Controller`, which inherits from `@Component` — and `@Component` is what's responsible for creating the bean/object.
- So `@RestController` *itself* isn't what creates the object — that's `@Component`'s job by inheritance.
- However, object creation alone isn't enough. If you removed `@RestController` (using plain `@Component` instead), the object would still be created, but **no request mapping would be registered**. `DispatcherServlet` would have no way to identify `getAll()` or map `GET /person/all` to it — so a matching request would fail to find a controller/method to invoke.

In short: `@Component` creates the object; `@RestController`/`@Controller` is what makes `DispatcherServlet` aware of it as a request-handling target.

---

## Up Next

Two pillars of Spring Boot to cover next:
- **IoC** — Inversion of Control
- **DI** — Dependency Injection
