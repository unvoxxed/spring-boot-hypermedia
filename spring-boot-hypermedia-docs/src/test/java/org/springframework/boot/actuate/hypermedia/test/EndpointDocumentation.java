package org.springframework.boot.actuate.hypermedia.test;

import static org.springframework.restdocs.RestDocumentation.document;
import static org.springframework.restdocs.RestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import groovy.text.Template;
import groovy.text.TemplateEngine;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringBootHypermediaApplication.class)
@WebAppConfiguration
@TestPropertySource(properties = { "spring.jackson.serialization.indent_output=true",
		"endpoints.health.sensitive=true", "endpoints.links.enabled=false" })
@DirtiesContext
public class EndpointDocumentation {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private MvcEndpoints mvcEndpoints;

	@Autowired
	@Qualifier("metricFilter")
	private Filter metricFilter;

	@Autowired
	@Qualifier("webRequestLoggingFilter")
	private Filter traceFilter;

	@Autowired
	private TemplateEngine templates;

	@Value("${org.springframework.restdocs.outputDir:${user.dir}/target/generated-snippets}")
	private String restdocsOutputDir;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		System.setProperty("org.springframework.restdocs.outputDir",
				this.restdocsOutputDir);
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.addFilters(this.metricFilter, this.traceFilter)
				.apply(documentationConfiguration()).build();
	}

	@Test
	public void endpoints() throws Exception {

		final File docs = new File("src/main/asciidoc");

		final Map<String, Object> model = new LinkedHashMap<String, Object>();
		final List<EndpointDoc> endpoints = new ArrayList<EndpointDoc>();
		model.put("endpoints", endpoints);
		for (MvcEndpoint endpoint : getEndpoints()) {
			final String endpointPath = StringUtils.hasText(endpoint.getPath()) ? endpoint
					.getPath() : "/";

					if (!endpointPath.equals("/hal")) {
						String output = endpointPath.substring(1);
						output = output.length() > 0 ? output : "./";
						this.mockMvc
						.perform(get(endpointPath).accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk()).andDo(document(output))
						.andDo(new ResultHandler() {
							@Override
							public void handle(MvcResult mvcResult) throws Exception {
								EndpointDoc endpoint = new EndpointDoc(docs, endpointPath);
								endpoints.add(endpoint);
							}
						});
					}
		}
		File file = new File(this.restdocsOutputDir + "/endpoints.adoc");
		file.getParentFile().mkdirs();
		PrintWriter writer = new PrintWriter(file, "UTF-8");
		try {
			Template template = this.templates.createTemplate(new File(
					"src/test/resources/templates/endpoints.adoc.tpl"));
			template.make(model).writeTo(writer);
		}
		finally {
			writer.close();
		}
	}

	private Collection<? extends MvcEndpoint> getEndpoints() {
		List<? extends MvcEndpoint> endpoints = new ArrayList<MvcEndpoint>(
				this.mvcEndpoints.getEndpoints());
		Collections.sort(endpoints, new Comparator<MvcEndpoint>() {
			@Override
			public int compare(MvcEndpoint o1, MvcEndpoint o2) {
				return o1.getPath().compareTo(o2.getPath());
			}
		});
		return endpoints;
	}

	public static class EndpointDoc {

		private String path;
		private String custom;
		private String title;

		public EndpointDoc(File rootDir, String path) {
			this.title = path;
			this.path = path.equals("/") ? "" : path;
			String custom = path.substring(1) + ".adoc";
			if (new File(rootDir, custom).exists()) {
				this.custom = custom;
			}
		}

		public String getTitle() {
			return this.title;
		}

		public String getPath() {
			return this.path;
		}

		public String getCustom() {
			return this.custom;
		}

	}

}
