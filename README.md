The purpose of this project is to show how hypermedia links and "API"
documentation for the endpoints can be added to an existing Spring
Boot Actuator application. It was developed in a hackathon session at
Unvoxxed London 2015.

Spring Boot Actuator adds a load of "standard" production-ready HTTP
endpoints to any web application it finds itself in. For instance
there is a "/health" endpoint and a "/metrics" endpoint which both do
obvious things. The problem is that a) the documentation for all these
endpoints is manually created and maintained, and doesn't really look
like "proper" documentation for a REST service, and b) there is no
hypermedia so the endpoints are not discoverable.

## Dependencies

[Spring HATEOAS](http://projects.spring.io/spring-hateoas/) is a
lower-level project that supports adding links (hypermedia) to HTTP
endpoints. The basic abstractions are `Link` and `Resource`, where a
`Resource` is a set of links plus the "normal" business data returned
from an HTTP endpoint.

[Spring Restdocs](http://projects.spring.io/spring-restdocs) is a
test-driven documentation generator for HTTP endpoints implemented
with Spring MVC. You write tests and make assertions and snippets are
generated that can then be included in human-friendly static
documentation using [Asciidoctor](https://asciidoctor.org).

The [HAL Browser](https://github.com/mikekelly/hal-browser) is a neat,
embeddable browser for JSON endpoints if they expose hypermedia in the
[HAL](http://stateless.co/hal_specification.html) format. Spring
HATEOAS can generate data in HAL, so if we get as far as adding links,
we can also browse them using this tool.

## Running the Application

Get Java and Maven (3) and do this on the command line:

```
$ mvn sring-boot:run
```

Visit the home page at http://localhost:8080 and you will see a list
of other links, e.g:

```
{
  "_links": {
    "links": {
      "href": "http://localhost:8080"
    }, 
    "health": {
      "href": "http://localhost:8080/health"
    },
    "mappings": {
      "href": "http://localhost:8080/mappings"
    },
...
  }
}
```

The HAL browser is available as http://localhost:8080/hal. Try it!