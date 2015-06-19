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

package org.springframework.boot.actuate.hypermedia.endpoints;

import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author Dave Syer
 *
 */
@ConfigurationProperties("endpoints.hal")
public class HalBrowserEndpoint extends WebMvcConfigurerAdapter implements MvcEndpoint {

	private String path = "/hal";

	private ManagementServerProperties management;

	private boolean sensitive;

	public HalBrowserEndpoint(ManagementServerProperties management) {
		this.management = management;
	}

	@RequestMapping("/")
	public String browse() {
		return "forward:" + this.management.getContextPath() + this.path
				+ "/browser.html";
	}

	@RequestMapping("")
	public String redirect() {
		return "redirect:" + this.management.getContextPath() + this.path + "/#"
				+ this.management.getContextPath();
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler(this.management.getContextPath() + this.path + "/**")
		.addResourceLocations(
				"classpath:/META-INF/resources/webjars/hal-browser/b7669f1-1/");
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	public void setSensitive(boolean sensitive) {
		this.sensitive = sensitive;
	}

	@Override
	public boolean isSensitive() {
		return this.sensitive;
	}

	@Override
	public Class<? extends Endpoint<?>> getEndpointType() {
		return null;
	}

}
