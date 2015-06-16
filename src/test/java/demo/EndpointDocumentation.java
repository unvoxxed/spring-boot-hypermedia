package demo;

import static org.springframework.restdocs.RestDocumentation.document;
import static org.springframework.restdocs.RestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.PrintWriter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringBootHypermediaApplication.class)
@WebIntegrationTest({"spring.jackson.serialization.indent_output=true"})
public class EndpointDocumentation {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    MvcEndpoints mvcEndpoints;

    private String restdocsOutputDir = System.getProperty("org.springframework.restdocs.outputDir");

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(documentationConfiguration())
                .alwaysDo(document("{method-name}/{step}/"))
                .build();
    }

    @Test
    public void endpoints() throws Exception {
        File file = new File(restdocsOutputDir + "/endpoints.adoc");
        file.getParentFile().mkdirs();
        final PrintWriter writer = new PrintWriter(file, "UTF-8");

        try {
            for (MvcEndpoint endpoint : mvcEndpoints.getEndpoints()) {
                final String endpointPath = endpoint.getPath();

                this.mockMvc.perform(get(endpointPath)
                        .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andDo(document(endpointPath))
                        .andDo(new ResultHandler() {
                            @Override
                            public void handle(MvcResult mvcResult) throws Exception {
                                writer.println("== " + endpointPath);
                                writer.println("include::{generated}/" + endpointPath + "/curl-request.adoc[]");
                                writer.println("include::{generated}/" + endpointPath + "/http-request.adoc[]");
                                writer.println("include::{generated}/" + endpointPath + "/http-response.adoc[]");
                            }
                        });
            }
        } finally {
            writer.close();
        }
    }

}
