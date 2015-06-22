package demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import demo.VanillaHypermediaIntegrationTests.SpringBootHypermediaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringBootHypermediaApplication.class)
@WebAppConfiguration
@DirtiesContext
public class VanillaHypermediaIntegrationTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private MvcEndpoints mvcEndpoints;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	public void links() throws Exception {
		this.mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk()).andExpect(jsonPath("$._links").exists());
	}

	@Test
	public void envValue() throws Exception {
		this.mockMvc.perform(get("/env/user.home").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk()).andExpect(jsonPath("$._links").doesNotExist());
	}

	@Test
	public void endpointsAllListed() throws Exception {
		for (MvcEndpoint endpoint : this.mvcEndpoints.getEndpoints()) {
			String path = endpoint.getPath();
			path = path.startsWith("/") ? path.substring(1) : path;
			this.mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$._links.%s.href", path).exists());
		}
	}

	@Test
	public void endpointsEachHaveSelf() throws Exception {
		for (MvcEndpoint endpoint : this.mvcEndpoints.getEndpoints()) {
			String path = endpoint.getPath();
			if ("/hal".equals(path)) {
				continue;
			}
			path = path.length()>0 ? path : "/";
			this.mockMvc
			.perform(get(path).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(
					jsonPath("$._links.self.href").value(
							"http://localhost" + endpoint.getPath()));
		}
	}

	@EnableAutoConfiguration
	@Configuration
	public static class SpringBootHypermediaApplication {

		public static void main(String[] args) {
			SpringApplication.run(SpringBootHypermediaApplication.class, args);
		}
	}

}
