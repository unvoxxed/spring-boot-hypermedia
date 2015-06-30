package org.springframework.boot.actuate.hypermedia.test;

import static org.springframework.restdocs.RestDocumentation.document;
import static org.springframework.restdocs.RestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import groovy.text.TemplateEngine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringBootHypermediaApplication.class)
@WebAppConfiguration
@TestPropertySource(properties = { "spring.jackson.serialization.indent_output=true",
"endpoints.health.sensitive=false" })
@DirtiesContext
public class HypermediaEndpointDocumentation {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private MvcEndpoints mvcEndpoints;

	@Autowired
	private TemplateEngine templates;

	@Value("${org.springframework.restdocs.outputDir:target/generated-snippets}")
	private String restdocsOutputDir;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		System.setProperty("org.springframework.restdocs.outputDir",
				this.restdocsOutputDir);
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration())
				.build();
	}

	@Test
	public void beans() throws Exception {
		this.mockMvc.perform(get("/beans").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andDo(document("beans/hypermedia"));
	}

	@Test
	public void metrics() throws Exception {
		this.mockMvc.perform(get("/metrics").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andDo(document("metrics/hypermedia"));
	}

	@Test
	public void home() throws Exception {
		this.mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andDo(document("admin"));
	}

}
