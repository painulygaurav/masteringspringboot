package com.example.demoapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class PersonController {

    private final Map<String, Person> persons;

    public PersonController() {
        this.persons = new HashMap<>();
        this.persons.put("1", new Person("Gaurav","Painuly"));
        this.persons.put("2", new Person("Shekhar", "Painuly"));
        this.persons.put("3", new Person("Sakshi", "Painuly"));
    }


    @GetMapping("/person/all")
    public List<Person> getAll(){
        return persons.values().stream().collect(Collectors.toList());
    }

    /**
     *
     * Applicaton startup:
     * 2026-09-25T18:24:12.627+05:30  INFO 14750 --- [demo-api] [           main] com.example.demoapi.DemoApiApplication   : Started DemoApiApplication in 0.578 seconds (process running for 0.839)
     * 2026-09-25T18:24:12.627+05:30 DEBUG 14750 --- [demo-api] [           main] o.s.b.a.ApplicationAvailabilityBean      : Application availability state LivenessState changed to CORRECT
     * 2026-09-25T18:24:12.628+05:30 DEBUG 14750 --- [demo-api] [           main] o.s.b.a.ApplicationAvailabilityBean      : Application availability state ReadinessState changed to ACCEPTING_TRAFFIC
     * 2026-09-25T18:25:09.177+05:30 DEBUG 14750 --- [demo-api] [nio-8080-exec-1] o.a.c.a.jaspic.AuthConfigFactoryImpl     : Loading persistent provider registrations from [/private/var/folders/sz/9t1lgsc914ldg1mzxxr2n_tr0000gp/T/tomcat.8080.4089483898362613192/conf/jaspic-providers.xml]
     * 2026-09-25T18:25:09.179+05:30  INFO 14750 --- [demo-api] [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
     * 2026-09-25T18:25:09.179+05:30  INFO 14750 --- [demo-api] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
     * 2026-09-25T18:25:09.179+05:30 DEBUG 14750 --- [demo-api] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Detected StandardServletMultipartResolver
     * 2026-09-25T18:25:09.179+05:30 DEBUG 14750 --- [demo-api] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Detected AcceptHeaderLocaleResolver
     * 2026-09-25T18:25:09.180+05:30 DEBUG 14750 --- [demo-api] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Detected org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator@5414d7f1
     * 2026-09-25T18:25:09.180+05:30 DEBUG 14750 --- [demo-api] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Detected org.springframework.web.servlet.support.SessionFlashMapManager@523df81e
     * 2026-09-25T18:25:09.181+05:30 DEBUG 14750 --- [demo-api] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : enableLoggingRequestDetails='false': request parameters and headers will be masked to prevent unsafe logging of potentially sensitive data
     * 2026-09-25T18:25:09.181+05:30  INFO 14750 --- [demo-api] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 2 ms
     *
     * Output or REST call:
     * 2026-09-25T18:07:51.841+05:30 DEBUG 73267 --- [demo-api] [nio-8080-exec-8] o.s.web.servlet.DispatcherServlet        : GET "/person/all", parameters={}
     * 2026-09-25T18:07:51.844+05:30 DEBUG 73267 --- [demo-api] [nio-8080-exec-8] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped to com.example.demoapi.PersonController#getAll()
     * 2026-09-25T18:07:51.848+05:30 DEBUG 73267 --- [demo-api] [nio-8080-exec-8] m.m.a.RequestResponseBodyMethodProcessor : Using 'application/json;q=0.8', given [text/html, application/xhtml+xml, image/avif, image/webp, image/apng, application/xml;q=0.9,
     * 2026-09-25T18:07:51.848+05:30 DEBUG 73267 --- [demo-api] [nio-8080-exec-8] m.m.a.RequestResponseBodyMethodProcessor : Writing [[com.example.demoapi.Person@1d1f2fdc, com.example.demoapi.Person@5aa945e, com.example.demoapi.Person (truncated)...]
     * 2026-09-25T18:07:51.850+05:30 DEBUG 73267 --- [demo-api] [nio-8080-exec-8] o.s.web.servlet.DispatcherServlet        : Completed 200 OK
     */
}
