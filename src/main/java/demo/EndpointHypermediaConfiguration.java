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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Dave Syer
 *
 */
@Configuration
@RestController
@EnableAspectJAutoProxy(proxyTargetClass=true)
public class EndpointHypermediaConfiguration {

	@Autowired
	MvcEndpoints endpoints;

	@RequestMapping("/")
	public ResourceSupport links() {
		ResourceSupport map = new ResourceSupport();
		map.add(linkTo(EndpointHypermediaConfiguration.class).withSelfRel());
		for (MvcEndpoint endpoint : this.endpoints.getEndpoints()) {
			map.add(linkTo(endpoint.getEndpointType()).slash(endpoint.getPath()).withRel(
					endpoint.getPath().substring(1)));
		}
		return map;
	}

	@Aspect
	@Component
	public static class WebEndpointPostProcessorConfiguration {
		@Around("execution(@org.springframework.web.bind.annotation.RequestMapping public * org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint+.*(..))")
		public Object enhance(ProceedingJoinPoint joinPoint) throws Throwable {
			return new EndpointResource(joinPoint.proceed(), (MvcEndpoint) joinPoint.getTarget());
		}
	}

}

class EndpointResource extends ResourceSupport {

	private Object embedded;

	@JsonCreator
	public EndpointResource(Object embedded, MvcEndpoint endpoint) {
		this.embedded = embedded;
		add(linkTo(endpoint.getEndpointType()).slash(endpoint.getPath().substring(1)).withSelfRel());
	}

	@JsonProperty("_embedded")
	public Object getEmbedded() {
		return this.embedded;
	}

}