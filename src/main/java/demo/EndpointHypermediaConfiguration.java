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
import java.util.Collection;
import java.util.Collections;

import javax.annotation.PostConstruct;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import endpoints.HalBrowserEndpoint;
import endpoints.LinksMvcEndpoint;

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

	@Bean
	public LinksMvcEndpoint linksMvcEndpoint(ManagementServerProperties management) {
		return new LinksMvcEndpoint(this.endpoints, management.getContextPath());
	}

	@Bean
	public HalBrowserEndpoint halBrowserMvcEndpoint(ManagementServerProperties management) {
		return new HalBrowserEndpoint(management);
	}

	@Aspect
	@Component
	public static class WebEndpointPostProcessorConfiguration {

		@Autowired
		ManagementServerProperties management;

		@Around("execution(@org.springframework.web.bind.annotation.RequestMapping public "
				+ "* org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint+.*(..))"
				+ " && !execution(* endpoints.LinksMvcEndpoint+.*(..))")
		public Object enhance(ProceedingJoinPoint joinPoint) throws Throwable {
			return new EndpointResource(joinPoint.proceed(),
					(MvcEndpoint) joinPoint.getTarget(), this.management.getContextPath());
		}
	}

	@Component
	public static class GenericEndpointPostProcessor {

		@Autowired
		MvcEndpoints endpoints;

		@Autowired
		ManagementServerProperties management;

		@PostConstruct
		public void init() {
			for (MvcEndpoint bean : this.endpoints.getEndpoints()) {
				if (bean instanceof EndpointMvcAdapter) {
					EndpointMvcAdapter adapter = (EndpointMvcAdapter) bean;
					GenericEndpointAdapter endpoint = new GenericEndpointAdapter(
							adapter.getDelegate(), adapter, this.management.getContextPath());
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
	private String rootPath;

	public GenericEndpointAdapter(Endpoint<?> endpoint, EndpointMvcAdapter generic,
			String rootPath) {
		this.delegate = endpoint;
		this.generic = generic;
		this.rootPath = rootPath;
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
		return new EndpointResource(this.delegate.invoke(), this.generic, this.rootPath);
	}

}

class EndpointResource extends ResourceSupport {

	private Object embedded;

	@JsonCreator
	public EndpointResource(Object embedded, MvcEndpoint endpoint, String rootPath) {
		this.embedded = embedded;
		Class<?> type = endpoint.getEndpointType();
		type = type == null ? Object.class : type;
		String rel = endpoint.getPath();
		rel = rel.startsWith("/") && !StringUtils.hasText(rootPath) ? rel.substring(1) : rel;
		add(linkTo(type).slash(rootPath + rel).withSelfRel());
	}

	@JsonProperty("_embedded")
	public Object getEmbedded() {
		if (this.embedded instanceof Collection) {
			return Collections.singletonMap("content", this.embedded);
		}
		return this.embedded;
	}

}