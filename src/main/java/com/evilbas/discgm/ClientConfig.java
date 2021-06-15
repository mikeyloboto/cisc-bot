package com.evilbas.discgm;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@PropertySources({ @PropertySource("classpath:application.properties"),
		@PropertySource("file:${APPLICATION_EXT_PROPERTIES}/discgm-ws/env.properties") })
@MapperScan("com.evilbas.discgm.dao.sql")
@ComponentScan(basePackages = "com.evilbas.discgm")
@EnableAsync
public class ClientConfig {

	private Logger log = LoggerFactory.getLogger(ClientConfig.class);

}
