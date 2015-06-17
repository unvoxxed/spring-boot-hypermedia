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

import java.lang.reflect.Field;

import javax.annotation.PostConstruct;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Dave Syer
 *
 */
@Configuration
@Controller
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class EndpointHypermediaConfiguration {

	@Autowired
	MvcEndpoints endpoints;

	@RequestMapping("/")
	@ResponseBody
	public ResourceSupport links() {
		ResourceSupport map = new ResourceSupport();
		map.add(linkTo(EndpointHypermediaConfiguration.class).withSelfRel());
		for (MvcEndpoint endpoint : this.endpoints.getEndpoints()) {
			map.add(linkTo(endpoint.getEndpointType()).slash(endpoint.getPath()).withRel(
					endpoint.getPath().substring(1)));
		}
		return map;
	}

	@RequestMapping("/hal")
	public String redirect() {
		return "redirect:/hal/";
	}

	@RequestMapping("/hal/")
	public String browse() {
		return "forward:/hal/browser.html";
	}

	@Component
	public static class HalConfigurer extends WebMvcConfigurerAdapter {
		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/hal/**").addResourceLocations("classpath:/META-INF/resources/webjars/hal-browser/b7669f1-1/");
		}
	}

	@Aspect
	@Component
	public static class WebEndpointPostProcessorConfiguration {
		@Around("execution(@org.springframework.web.bind.annotation.RequestMapping public "
				+ "* org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint+.*(..))")
		public Object enhance(ProceedingJoinPoint joinPoint) throws Throwable {
			return new EndpointResource(joinPoint.proceed(),
					(MvcEndpoint) joinPoint.getTarget());
		}
	}

	@Component
	public static class GenericEndpointPostProcessor {

		@Autowired
		MvcEndpoints endpoints;

		@PostConstruct
		public void init() {
			for (MvcEndpoint bean : this.endpoints.getEndpoints()) {
				if (bean instanceof EndpointMvcAdapter) {
					EndpointMvcAdapter adapter = (EndpointMvcAdapter) bean;
					GenericEndpointAdapter endpoint = new GenericEndpointAdapter(
							adapter.getDelegate(), adapter);
					/*
					 * This works, but it is fragile (reflection)
					 */
					Field field = ReflectionUtils.findField(EndpointMvcAdapter.class,
							"delegate");
					ReflectionUtils.makeAccessible(field);
					ReflectionUtils.setField(field, adapter, endpoint);
				}
			}
		}

	}

}

class GenericEndpointAdapter implements Endpoint<EndpointResource> {

	private Endpoint<?> delegate;
	private EndpointMvcAdapter generic;

	public GenericEndpointAdapter(Endpoint<?> endpoint, EndpointMvcAdapter generic) {
		this.delegate = endpoint;
		this.generic = generic;
	}

	@Override
	public String getId() {
		return this.delegate.getId();
	}

	@Override
	public boolean isEnabled() {
		return this.delegate.isEnabled();
	}

	@Override
	public boolean isSensitive() {
		return this.delegate.isSensitive();
	}

	@Override
	public EndpointResource invoke() {
		return new EndpointResource(this.delegate.invoke(), this.generic);
	}

}

class EndpointResource extends ResourceSupport {

	private Object embedded;

	@JsonCreator
	public EndpointResource(Object embedded, MvcEndpoint endpoint) {
		this.embedded = embedded;
		add(linkTo(endpoint.getEndpointType()).slash(endpoint.getPath().substring(1))
				.withSelfRel());
	}

	@JsonProperty("_embedded")
	public Object getEmbedded() {
		return this.embedded;
	}

}