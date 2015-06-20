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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.actuate.hypermedia.endpoints.HalBrowserEndpoint;
import org.springframework.boot.actuate.hypermedia.endpoints.LinksMvcEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Dave Syer
 *
 */
@Configuration
@AutoConfigureAfter(HypermediaAutoConfiguration.class)
public class EndpointHypermediaAutoConfiguration {

	@Bean
	public LinksMvcEndpoint linksMvcEndpoint(BeanFactory beanFactory,
			ManagementServerProperties management) {
		return new LinksMvcEndpoint(beanFactory, management.getContextPath());
	}

	@Bean
	@ConditionalOnResource(resources = "classpath:/META-INF/resources/webjars/hal-browser/b7669f1-1")
	public HalBrowserEndpoint halBrowserMvcEndpoint(ManagementServerProperties management) {
		return new HalBrowserEndpoint(management);
	}

	@ControllerAdvice(assignableTypes = MvcEndpoint.class)
	@Component
	public static class WebEndpointPostProcessorConfiguration implements
	ResponseBodyAdvice<Object> {

		@Autowired
		ManagementServerProperties management;

		@Autowired
		MvcEndpoints endpoints;

		@Autowired
		ObjectMapper mapper;

		@Override
		public boolean supports(MethodParameter returnType,
				Class<? extends HttpMessageConverter<?>> converterType) {
			Class<?> controllerType = returnType.getDeclaringClass();
			return !LinksMvcEndpoint.class.isAssignableFrom(controllerType)
					&& !HalBrowserEndpoint.class.isAssignableFrom(controllerType);
		}

		@Override
		public Object beforeBodyWrite(Object body, MethodParameter returnType,
				MediaType selectedContentType,
				Class<? extends HttpMessageConverter<?>> selectedConverterType,
						ServerHttpRequest request, ServerHttpResponse response) {
			Class<?> controllerType = returnType.getDeclaringClass();
			return new EndpointResource(body, this.mapper, findPath(controllerType),
					this.management.getContextPath());
		}

		private String findPath(Class<?> controllerType) {
			for (MvcEndpoint endpoint : this.endpoints.getEndpoints()) {
				if (controllerType.isAssignableFrom(endpoint.getClass())) {
					return endpoint.getPath();
				}
			}
			return "";
		}

	}

}

class EndpointResource extends ResourceSupport {

	private Object embedded;
	private Map<String, Object> details = new LinkedHashMap<String, Object>();
	private String rel;
	private ObjectMapper mapper;

	@JsonCreator
	public EndpointResource(Object embedded, ObjectMapper mapper, String path,
			String rootPath) {
		this.embedded = embedded;
		this.mapper = mapper;
		this.rel = path;
		this.rel = this.rel.startsWith("/") && !StringUtils.hasText(rootPath) ? this.rel
				.substring(1) : this.rel;
				add(linkTo(Object.class).slash(rootPath + this.rel).withSelfRel());
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