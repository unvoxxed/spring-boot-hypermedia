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

package endpoints;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import autoconfigure.EndpointHypermediaAutoConfiguration;

/**
 * @author Dave Syer
 *
 */
@ConfigurationProperties("endpoints.links")
public class LinksMvcEndpoint implements MvcEndpoint {

	private String path = "";

	private MvcEndpoints endpoints;

	private String rootPath;

	private BeanFactory beanFactory;

	public LinksMvcEndpoint(BeanFactory beanFactory, String rootPath) {
		this.beanFactory = beanFactory;
		this.rootPath = rootPath;
	}

	@RequestMapping(value = { "/", "" })
	@ResponseBody
	public ResourceSupport links() {
		ResourceSupport map = new ResourceSupport();
		String rel = this.path.startsWith("/") ? this.path.substring(1) : StringUtils
				.hasText(this.path) ? this.path : "links";
		map.add(linkTo(EndpointHypermediaAutoConfiguration.class).slash(
				this.rootPath + getPath()).withRel(rel));
		for (MvcEndpoint endpoint : getEndpoints()) {
			if (endpoint.getPath().equals(this.getPath())) {
				continue;
			}
			Class<?> type = endpoint.getEndpointType();
			if (type == null) {
				type = Object.class;
			}
			map.add(linkTo(type).slash(this.rootPath + endpoint.getPath()).withRel(
					endpoint.getPath().substring(1)));
		}
		return map;
	}

	private Set<? extends MvcEndpoint> getEndpoints() {
		if (this.endpoints == null) {
			this.endpoints = this.beanFactory.getBean(MvcEndpoints.class);
		}
		return this.endpoints.getEndpoints();
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public boolean isSensitive() {
		return false;
	}

	@Override
	public Class<? extends Endpoint<?>> getEndpointType() {
		return null;
	}

}
