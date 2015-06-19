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

package org.springframework.boot.actuate.hypermedia.autoconfigure;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.actuate.hypermedia.endpoints.HalBrowserEndpoint;
import org.springframework.boot.actuate.hypermedia.endpoints.LinksMvcEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Dave Syer
 *
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@AutoConfigureAfter(HypermediaAutoConfiguration.class)
@AutoConfigureBefore(AopAutoConfiguration.class)
public class EndpointHypermediaAutoConfiguration {

	@Bean
	public LinksMvcEndpoint linksMvcEndpoint(BeanFactory beanFactory, ManagementServerProperties management) {
		return new LinksMvcEndpoint(beanFactory, management.getContextPath());
	}

	@Bean
	@ConditionalOnResource(resources = "classpath:/META-INF/resources/webjars/hal-browser/b7669f1-1")
	public HalBrowserEndpoint halBrowserMvcEndpoint(ManagementServerProperties management) {
		return new HalBrowserEndpoint(management);
	}

	@Aspect
	@Component
	public static class WebEndpointPostProcessorConfiguration {

		@Autowired
		ManagementServerProperties management;

		@Autowired
		ObjectMapper mapper;

		@Around("execution(@org.springframework.web.bind.annotation.RequestMapping public "
				+ "* org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint+.*(..))"
				+ " && !execution(* org.springframework.boot.actuate.hypermedia.endpoints.LinksMvcEndpoint+.*(..))"
				+ " && !execution(* org.springframework.boot.actuate.hypermedia.endpoints.HalBrowserEndpoint+.*(..))")
		public Object enhance(ProceedingJoinPoint joinPoint) throws Throwable {
			return new EndpointResource(joinPoint.proceed(), this.mapper,
					(MvcEndpoint) joinPoint.getTarget(), this.management.getContextPath());
		}
	}

	@Component
	public static class GenericEndpointPostProcessor {

		@Autowired
		MvcEndpoints endpoints;

		@Autowired
		ObjectMapper mapper;

		@Autowired
		ManagementServerProperties management;

		@PostConstruct
		public void init() {
			for (MvcEndpoint bean : this.endpoints.getEndpoints()) {
				if (bean instanceof EndpointMvcAdapter) {
					EndpointMvcAdapter adapter = (EndpointMvcAdapter) bean;
					GenericEndpointAdapter endpoint = new GenericEndpointAdapter(
							adapter.getDelegate(), adapter, this.mapper,
							this.management.getContextPath());
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
	private ObjectMapper mapper;

	public GenericEndpointAdapter(Endpoint<?> endpoint, EndpointMvcAdapter generic,
			ObjectMapper mapper, String rootPath) {
		this.delegate = endpoint;
		this.generic = generic;
		this.mapper = mapper;
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
		return new EndpointResource(this.delegate.invoke(), this.mapper, this.generic,
				this.rootPath);
	}

}

class EndpointResource extends ResourceSupport {

	private Object embedded;
	private Map<String, Object> details = new LinkedHashMap<String, Object>();
	private String rel;
	private ObjectMapper mapper;

	@JsonCreator
	public EndpointResource(Object embedded, ObjectMapper mapper, MvcEndpoint endpoint,
			String rootPath) {
		this.embedded = embedded;
		this.mapper = mapper;
		Class<?> type = endpoint.getEndpointType();
		type = type == null ? Object.class : type;
		this.rel = endpoint.getPath();
		this.rel = this.rel.startsWith("/") && !StringUtils.hasText(rootPath) ? this.rel
				.substring(1) : this.rel;
				add(linkTo(type).slash(rootPath + this.rel).withSelfRel());
				flatten();
	}

	@JsonAnyGetter
	public Map<String, Object> getDetails() {
		return this.details;
	}

	@SuppressWarnings("unchecked")
	private void flatten() {
		if (this.embedded instanceof Collection) {
			this.details.put(this.rel, this.embedded);
		}
		else if (this.embedded instanceof Map) {
			this.details.putAll((Map<String, Object>) this.embedded);
		}
		else {
			this.details.putAll(this.mapper.convertValue(this.embedded, Map.class));
		}
	}

}