package com.neusoft.rsapm.nilebeat.configuration;

import com.typesafe.config.Config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@EqualsAndHashCode
public class HttpEndPointConfiuration implements EndpointConfiguration {
	private String path;
	
	public HttpEndPointConfiuration(Config config) {
		this.path = config.getString("url");
	}
}
