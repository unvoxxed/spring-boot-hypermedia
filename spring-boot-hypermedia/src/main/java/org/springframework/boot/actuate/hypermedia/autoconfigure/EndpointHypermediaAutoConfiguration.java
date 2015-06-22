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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.actuate.hypermedia.endpoints.HalBrowserEndpoint;
import org.springframework.boot.actuate.hypermedia.endpoints.LinksEnhancer;
import org.springframework.boot.actuate.hypermedia.endpoints.LinksMvcEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.MethodParameter;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.TypeUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Dave Syer
 *
 */
@Configuration
@ConditionalOnClass(Link.class)
@ConditionalOnWebApplication
@ConditionalOnProperty(value = "endpoints.links.enabled", matchIfMissing = true)
@AutoConfigureAfter(HypermediaAutoConfiguration.class)
public class EndpointHypermediaAutoConfiguration {

	@Bean
	public LinksMvcEndpoint linksMvcEndpoint(BeanFactory beanFactory,
			ManagementServerProperties management) {
		return new LinksMvcEndpoint();
	}

	@Bean
	@ConditionalOnProperty(value = "endpoints.hal.enabled", matchIfMissing = true)
	@ConditionalOnResource(resources = "classpath:/META-INF/resources/webjars/hal-browser/b7669f1-1")
	public HalBrowserEndpoint halBrowserMvcEndpoint(ManagementServerProperties management) {
		return new HalBrowserEndpoint(management);
	}

	/**
	 * Controller advice that adds links to the home page and/or the management context
	 * path. The home page is enhanced if it is composed already of a
	 * {@link ResourceSupport} (e.g. when using Spring Data REST).
	 *
	 * @author Dave Syer
	 *
	 */
	@ControllerAdvice
	@Component
	@Scope("request")
	public static class HomePageLinksAdvice implements ResponseBodyAdvice<Object> {

		@Autowired
		HttpServletRequest servletRequest;

		@Autowired
		MvcEndpoints endpoints;

		@Autowired
		ManagementServerProperties management;

		private LinksEnhancer linksEnhancer;

		@PostConstruct
		public void init() {
			this.linksEnhancer = new LinksEnhancer(this.endpoints,
					this.management.getContextPath());
		}

		@Override
		public boolean supports(MethodParameter returnType,
				Class<? extends HttpMessageConverter<?>> converterType) {
			Class<?> controllerType = returnType.getDeclaringClass();
			if (!LinksMvcEndpoint.class.isAssignableFrom(controllerType)
					&& MvcEndpoint.class.isAssignableFrom(controllerType)) {
				return false;
			}
			returnType.increaseNestingLevel();
			Type nestedType = returnType.getNestedGenericParameterType();
			returnType.decreaseNestingLevel();
			return ResourceSupport.class.isAssignableFrom(returnType.getParameterType())
					|| TypeUtils.isAssignable(ResourceSupport.class, nestedType);
		}

		@Override
		public Object beforeBodyWrite(Object body, MethodParameter returnType,
				MediaType selectedContentType,
				Class<? extends HttpMessageConverter<?>> selectedConverterType,
						ServerHttpRequest request, ServerHttpResponse response) {
			Object pattern = this.servletRequest
					.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
			if (pattern != null) {
				String path = pattern.toString();
				if (isHomePage(path) || isManagementPath(path)) {
					ResourceSupport resource = (ResourceSupport) body;
					if (isHomePage(path) && hasManagementPath()) {
						String rel = this.management.getContextPath().substring(1);
						resource.add(linkTo(EndpointHypermediaAutoConfiguration.class)
								.slash(this.management.getContextPath()).withRel(rel));
					}
					else {
						this.linksEnhancer.addEndpointLinks(resource, "");
					}
				}
			}
			return body;
		}

		private boolean hasManagementPath() {
			return StringUtils.hasText(this.management.getContextPath());
		}

		private boolean isManagementPath(String path) {
			return this.management.getContextPath().equals(path);
		}

		private boolean isHomePage(String path) {
			return "".equals("path") || "/".equals(path);
		}

	}

	/**
	 * Controller advice that adds links to the existing Actuator endpoints. By default
	 * all the top-level resources are enhanced with a "self" link. Those resources that
	 * could not be enhanced (e.g. "/env/{name}") because their values are "primitive" are
	 * ignored. Those that have values of type Collection (e.g. /trace) are transformed in
	 * to maps, and the original collection value is added with a key equal to the
	 * endpoint name.
	 *
	 * @author Dave Syer
	 *
	 */
	@ControllerAdvice(assignableTypes = MvcEndpoint.class)
	@Component
	@Scope("request")
	public static class MvcEndpointAdvice implements ResponseBodyAdvice<Object> {

		@Autowired
		HttpServletRequest servletRequest;

		@Autowired
		ManagementServerProperties management;

		@Autowired
		MvcEndpoints endpoints;

		@Autowired
		HttpMessageConverters converters;

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

			HttpMessageConverter<?> converter = findConverter(selectedConverterType,
					getReturnType(body, returnType), selectedContentType);
			if (converter != null
					&& !converter.canWrite(ResourceSupport.class, selectedContentType)) {
				// Not a resource that can be enhanced with a link
				return body;
			}

			String path = "";
			Object pattern = this.servletRequest
					.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
			if (pattern != null) {
				path = pattern.toString();
			}

			return new EndpointResource(body, this.mapper, path,
					this.management.getContextPath());

		}

		private HttpMessageConverter<?> findConverter(
				Class<? extends HttpMessageConverter<?>> selectedConverterType,
						Class<?> returnType, MediaType mediaType) {
			for (HttpMessageConverter<?> converter : this.converters) {
				if (selectedConverterType.isAssignableFrom(converter.getClass())
						&& converter.canWrite(returnType, mediaType)) {
					return converter;
				}
			}
			return null;
		}

		private Class<?> getReturnType(Object body, MethodParameter returnType) {
			return body != null ? body.getClass() : returnType.getParameterType();
		}

	}

	private static class EndpointResource extends ResourceSupport {

		private Object embedded;
		private Map<String, Object> details = new LinkedHashMap<String, Object>();
		private String property;
		private ObjectMapper mapper;

		@JsonCreator
		public EndpointResource(Object embedded, ObjectMapper mapper, String path,
				String rootPath) {
			this.embedded = embedded;
			this.mapper = mapper;
			this.property = path.substring(rootPath.length());
			this.property = this.property.startsWith("/") ? this.property.substring(1) : this.property;
			add(linkTo(Object.class).slash(path).withSelfRel());
			flatten();
		}

		@JsonAnyGetter
		public Map<String, Object> getDetails() {
			return this.details;
		}

		@SuppressWarnings("unchecked")
		private void flatten() {
			if (this.embedded instanceof Collection) {
				this.details.put(this.property, this.embedded);
			}
			else if (this.embedded instanceof Map) {
				this.details.putAll((Map<String, Object>) this.embedded);
			}
			else {
				try {
					this.details.putAll(this.mapper
							.convertValue(this.embedded, Map.class));
				}
				catch (Exception e) {
					this.details.put("value", this.embedded);
				}
			}
		}

	}

}
