/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package demo;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Dave Syer
 *
 */
@Configuration
@RestController
public class EndpointHypermediaConfiguration {

	@Autowired
	MvcEndpoints endpoints;

	@RequestMapping("/")
	public Map<String, Object> links() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("self", linkTo(EndpointHypermediaConfiguration.class).withSelfRel());
		for (MvcEndpoint endpoint : this.endpoints.getEndpoints()) {
			map.put(endpoint.getPath().substring(1),
					linkTo(HealthMvcEndpoint.class).slash(endpoint.getPath()).withSelfRel());
		}
		return Collections.<String, Object> singletonMap("_links", map);
	}

}
