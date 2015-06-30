package org.springframework.boot.actuate.hypermedia.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.hypermedia.test.ServerPortHypermediaIntegrationTests.SpringBootHypermediaApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringBootHypermediaApplication.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "management.port=0" })
@DirtiesContext
public class ServerPortHypermediaIntegrationTests {

	@Value("${local.management.port}")
	private int port;

	@Test
	public void links() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		ResponseEntity<String> entity = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/", HttpMethod.GET,
				new HttpEntity<Void>(null, headers), String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertTrue("Wrong body: " + entity.getBody(),
				entity.getBody().contains("\"_links\":"));
	}

	@Test
	public void browser() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		ResponseEntity<String> entity = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/", HttpMethod.GET,
				new HttpEntity<Void>(null, headers), String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertTrue("Wrong body: " + entity.getBody(),
				entity.getBody().contains("<title"));
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	public static class SpringBootHypermediaApplication {

		@RequestMapping("")
		public ResourceSupport home() {
			ResourceSupport resource = new ResourceSupport();
			resource.add(linkTo(SpringBootHypermediaApplication.class).slash("/")
					.withSelfRel());
			return resource;
		}

		public static void main(String[] args) {
			new SpringApplicationBuilder(SpringBootHypermediaApplication.class)
			.properties("management.port:9000").run(args);
		}
	}

}
